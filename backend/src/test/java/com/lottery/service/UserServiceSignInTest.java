package com.lottery.service;

import com.lottery.config.ClockConfig;
import com.lottery.config.DataInitializer;
import com.lottery.dto.SignInDTO;
import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每日签到功能测试
 *
 * <p>使用可控的 {@link MutableClock} 固定业务时区（Asia/Shanghai），
 * 覆盖签到成功、重复签到、非普通用户、用户不存在、用户被禁用、并发竞态、
 * 固定时区与跨日边界等场景。</p>
 */
@SpringBootTest
@Import(UserServiceSignInTest.TestClockConfig.class)
class UserServiceSignInTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MutableClock clock;

    /**
     * 替换默认数据初始化器，避免其种子数据与本测试无关的校验干扰签到测试的上下文加载
     */
    @MockBean
    private DataInitializer dataInitializer;

    @BeforeEach
    void resetClock() {
        // 默认：2026-07-25 12:00 (Asia/Shanghai)，对应业务日期 2026-07-25
        clock.setInstant(Instant.parse("2026-07-25T04:00:00Z"));
    }

    /**
     * 创建测试用户（用户名保证唯一，避免与初始化数据冲突）
     */
    private User createUser(String role, boolean enabled) {
        User user = User.builder()
                .username("signin_" + System.nanoTime())
                .password("pwd123")
                .nickname("签到测试用户")
                .remainingChances(5)
                .role(role)
                .enabled(enabled)
                .build();
        return userRepository.save(user);
    }

    @Test
    void signIn_success_increasesChancesAndMarksSignedIn() {
        User user = createUser("USER", true);

        SignInDTO result = userService.signIn(user.getId());

        assertTrue(result.isSignedInToday());
        assertEquals(LocalDate.of(2026, 7, 25), result.getLastSignInDate());
        assertEquals(6, result.getRemainingChances());

        // 状态查询与签到结果保持一致
        SignInDTO status = userService.getSignInStatus(user.getId());
        assertTrue(status.isSignedInToday());
        assertEquals(6, status.getRemainingChances());
    }

    @Test
    void signIn_duplicateSameDay_throwsAndKeepsChances() {
        User user = createUser("USER", true);

        userService.signIn(user.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.signIn(user.getId()));
        assertEquals("今日已签到，请明天再来", ex.getMessage());

        // 重复签到不应再增加次数
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(6, reloaded.getRemainingChances());
    }

    @Test
    void signIn_adminUser_rejectedAndKeepsChances() {
        User admin = createUser("ADMIN", true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.signIn(admin.getId()));
        assertEquals("仅普通用户可参与签到", ex.getMessage());

        // 管理员不应签到成功
        User reloaded = userRepository.findById(admin.getId()).orElseThrow();
        assertEquals(5, reloaded.getRemainingChances());
        assertNull(reloaded.getLastSignInDate());
    }

    @Test
    void signIn_userNotFound_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.signIn(999999L));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void signIn_userDisabled_throws() {
        User user = createUser("USER", false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.signIn(user.getId()));
        assertEquals("用户已被禁用", ex.getMessage());

        // 被禁用用户不应签到成功
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(5, reloaded.getRemainingChances());
        assertNull(reloaded.getLastSignInDate());
    }

    @Test
    void getSignInStatus_beforeSignIn_returnsNotSignedIn() {
        User user = createUser("USER", true);

        SignInDTO status = userService.getSignInStatus(user.getId());

        assertFalse(status.isSignedInToday());
        assertNull(status.getLastSignInDate());
        assertEquals(5, status.getRemainingChances());
    }

    /**
     * 固定时区：签到日期应基于 Asia/Shanghai 计算，而非 UTC 或 JVM 默认时区。
     */
    @Test
    void signIn_usesBusinessTimeZone_notUtc() {
        // 2026-07-25 20:00 UTC ⇒ Asia/Shanghai 为 2026-07-26 04:00
        clock.setInstant(Instant.parse("2026-07-25T20:00:00Z"));
        User user = createUser("USER", true);

        SignInDTO result = userService.signIn(user.getId());

        // 若按 UTC 计算会得到 07-25，按业务时区应为 07-26
        assertEquals(LocalDate.of(2026, 7, 26), result.getLastSignInDate());
    }

    /**
     * 跨日边界：Asia/Shanghai 跨过午夜后，次日应可再次签到成功。
     */
    @Test
    void signIn_crossDayBoundary_allowsNextDay() {
        User user = createUser("USER", true);

        // 2026-07-25 23:30 (Asia/Shanghai)
        clock.setInstant(Instant.parse("2026-07-25T15:30:00Z"));
        SignInDTO first = userService.signIn(user.getId());
        assertEquals(LocalDate.of(2026, 7, 25), first.getLastSignInDate());
        assertEquals(6, first.getRemainingChances());

        // 同一业务日再次签到应失败
        assertThrows(BusinessException.class, () -> userService.signIn(user.getId()));

        // 跨过午夜到 2026-07-26 00:30 (Asia/Shanghai)
        clock.setInstant(Instant.parse("2026-07-25T16:30:00Z"));
        SignInDTO second = userService.signIn(user.getId());
        assertEquals(LocalDate.of(2026, 7, 26), second.getLastSignInDate());
        assertEquals(7, second.getRemainingChances());
    }

    @Test
    void signIn_concurrent_onlyOneSucceeds() throws InterruptedException {
        User user = createUser("USER", true);
        Long userId = user.getId();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    userService.signIn(userId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // 并发下的乐观/唯一约束异常同样视为签到失败
                    failureCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "并发签到未在预期时间内完成");
        executor.shutdownNow();

        // 并发场景下每天只能签到成功一次
        assertEquals(1, successCount.get(), "并发签到应只有一次成功");
        assertEquals(threadCount - 1, failureCount.get());

        User reloaded = userRepository.findById(userId).orElseThrow();
        assertEquals(6, reloaded.getRemainingChances(), "并发签到只应增加一次抽奖次数");
    }

    /**
     * 测试用可变时钟：固定业务时区，可在测试中调整当前瞬间。
     */
    static class MutableClock extends Clock {
        private volatile Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-07-25T04:00:00Z"), ClockConfig.BUSINESS_ZONE);
        }
    }
}
