package com.lottery.service;

import com.lottery.config.DataInitializer;
import com.lottery.dto.CheckinResultDTO;
import com.lottery.entity.Prize;
import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.CheckinRecordRepository;
import com.lottery.repository.LotteryRecordRepository;
import com.lottery.repository.PrizeRepository;
import com.lottery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceCheckinTest {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @MockBean
    private DataInitializer dataInitializer;

    @MockBean
    private Clock clock;

    @Autowired
    private UserService userService;

    @Autowired
    private LotteryService lotteryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrizeRepository prizeRepository;

    @Autowired
    private CheckinRecordRepository checkinRecordRepository;

    @Autowired
    private LotteryRecordRepository lotteryRecordRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        lotteryRecordRepository.deleteAll();
        checkinRecordRepository.deleteAll();
        prizeRepository.deleteAll();
        userRepository.deleteAll();

        Clock systemClock = Clock.system(SHANGHAI);
        when(clock.getZone()).thenReturn(systemClock.getZone());
        when(clock.instant()).thenAnswer(inv -> systemClock.instant());

        testUser = User.builder()
                .username("testuser_" + System.nanoTime())
                .password("password")
                .nickname("测试用户")
                .remainingChances(5)
                .role("USER")
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    private void setFixedClock(String utcDateTime) {
        Clock fixed = Clock.fixed(Instant.parse(utcDateTime), SHANGHAI);
        when(clock.instant()).thenReturn(fixed.instant());
        when(clock.getZone()).thenReturn(fixed.getZone());
    }

    private Prize createPrize(String name, int stock) {
        Prize prize = Prize.builder()
                .name(name)
                .description("测试奖品")
                .probability(1000)
                .stock(stock)
                .prizeLevel(5)
                .color("#87CEEB")
                .enabled(true)
                .sortOrder(1)
                .build();
        return prizeRepository.save(prize);
    }

    @Test
    @DisplayName("签到成功 - 普通用户每日签到，抽奖次数+1")
    void dailyCheckin_success() {
        int chancesBefore = testUser.getRemainingChances();

        CheckinResultDTO result = userService.dailyCheckin(testUser.getId());

        assertTrue(result.getCheckedIn());
        assertEquals(1, result.getRewardedChances());
        assertEquals(chancesBefore + 1, result.getRemainingChances());
        assertTrue(result.getMessage().contains("签到成功"));

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(chancesBefore + 1, updated.getRemainingChances());

        boolean exists = checkinRecordRepository.existsByUserIdAndCheckinDate(
                testUser.getId(), LocalDate.now(clock));
        assertTrue(exists);
    }

    @Test
    @DisplayName("重复签到 - 同一用户当天重复签到返回明确提示")
    void dailyCheckin_duplicate() {
        userService.dailyCheckin(testUser.getId());
        int chancesBefore = userRepository.findById(testUser.getId()).orElseThrow().getRemainingChances();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(testUser.getId()));
        assertTrue(ex.getMessage().contains("已签到"));

        User after = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(chancesBefore, after.getRemainingChances());
    }

    @Test
    @DisplayName("用户不存在 - 签到时用户ID不存在抛出异常")
    void dailyCheckin_userNotExists() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(99999L));
        assertTrue(ex.getMessage().contains("用户不存在"));
    }

    @Test
    @DisplayName("用户被禁用 - 禁用用户签到时返回提示")
    void dailyCheckin_userDisabled() {
        testUser.setEnabled(false);
        userRepository.save(testUser);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(testUser.getId()));
        assertTrue(ex.getMessage().contains("已被禁用"));
    }

    @Test
    @DisplayName("管理员签到 - ADMIN角色签到被白名单拒绝")
    void dailyCheckin_adminRejected() {
        assertNonUserRoleRejected("ADMIN", 999);
    }

    @Test
    @DisplayName("非USER角色签到 - 任意自定义角色被白名单拒绝")
    void dailyCheckin_otherRoleRejected() {
        assertNonUserRoleRejected("GUEST", 10);
    }

    private void assertNonUserRoleRejected(String role, int chances) {
        User u = User.builder()
                .username("roleuser_" + System.nanoTime())
                .password("pw")
                .nickname("非普通用户")
                .remainingChances(chances)
                .role(role)
                .enabled(true)
                .build();
        u = userRepository.save(u);
        final Long uid = u.getId();
        int chancesBefore = u.getRemainingChances();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(uid));
        assertTrue(ex.getMessage().contains("普通用户"));

        User after = userRepository.findById(uid).orElseThrow();
        assertEquals(chancesBefore, after.getRemainingChances());
        assertFalse(checkinRecordRepository.existsByUserIdAndCheckinDate(uid, LocalDate.now(clock)));
    }

    @Test
    @DisplayName("并发签到 - 多线程并发签到仅一次成功")
    void dailyCheckin_concurrent() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userService.dailyCheckin(testUser.getId());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getMessage().contains("已签到")) {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "并发签到应只有一次成功");
        assertEquals(threadCount - 1, failCount.get(), "其余线程应失败");

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(6, updated.getRemainingChances(), "抽奖次数应只增加1次");
        assertEquals(1, checkinRecordRepository.count(), "应只有一条签到记录");
    }

    @Test
    @DisplayName("签到与充值并发 - 签到增加1次与后台充值增加次数不互相覆盖")
    void dailyCheckin_concurrentWithRecharge() throws InterruptedException {
        int initialChances = testUser.getRemainingChances();
        int rechargeCount = 3;
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (idx == 0) {
                        userService.dailyCheckin(testUser.getId());
                    } else {
                        userService.addChances(testUser.getId(), rechargeCount);
                    }
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(2, successCount.get());
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(initialChances + 1 + rechargeCount, updated.getRemainingChances(),
                "签到+1和充值+" + rechargeCount + "应同时生效，不互相覆盖");
        assertEquals(1, checkinRecordRepository.count());
    }

    @Test
    @DisplayName("签到与抽奖并发 - 签到增加次数与抽奖扣减次数不互相覆盖")
    void dailyCheckin_concurrentWithDraw() throws InterruptedException {
        createPrize("测试奖品", 100);
        int initialChances = testUser.getRemainingChances();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger checkinOk = new AtomicInteger(0);
        AtomicInteger drawOk = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                userService.dailyCheckin(testUser.getId());
                checkinOk.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                lotteryService.draw(testUser.getId(), "127.0.0.1");
                drawOk.incrementAndGet();
            } catch (BusinessException e) {
                // 抽奖可能因竞争失败，可接受
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        // 签到+1，抽奖成功则-1。若抽奖成功则次数不变；若抽奖因竞争失败则次数+1
        if (drawOk.get() == 1) {
            assertEquals(initialChances, updated.getRemainingChances(),
                    "签到+1和抽奖-1应同时生效");
            assertEquals(1, lotteryRecordRepository.count(), "应有一条抽奖记录");
        } else {
            assertEquals(initialChances + 1, updated.getRemainingChances(),
                    "签到+1生效，抽奖竞争失败次数不扣减");
        }
        assertEquals(1, checkinRecordRepository.count(), "应有一条签到记录");
    }

    @Test
    @DisplayName("跨日签到 - 次日可再次签到且次数正确累加")
    void dailyCheckin_crossDay() {
        setFixedClock("2026-07-24T02:00:00Z");
        CheckinResultDTO day1 = userService.dailyCheckin(testUser.getId());
        assertEquals(6, day1.getRemainingChances());
        assertTrue(day1.getCheckedIn());

        BusinessException dup = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(testUser.getId()));
        assertTrue(dup.getMessage().contains("已签到"));

        setFixedClock("2026-07-25T02:00:00Z");

        CheckinResultDTO statusDay2 = userService.getCheckinStatus(testUser.getId());
        assertFalse(statusDay2.getCheckedIn(), "次日应显示未签到");

        CheckinResultDTO day2 = userService.dailyCheckin(testUser.getId());
        assertTrue(day2.getCheckedIn());
        assertEquals(7, day2.getRemainingChances(), "次日签到应在6的基础上+1");

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(7, updated.getRemainingChances());
        assertEquals(2, checkinRecordRepository.count(), "应有两条签到记录");
    }

    @Test
    @DisplayName("时区边界 - UTC 23:00 对应上海次日07:00，签到日期按上海时区计算")
    void dailyCheckin_timezoneBoundary() {
        // UTC 2026-07-24T23:00:00Z = 上海 2026-07-25T07:00:00+08:00
        setFixedClock("2026-07-24T23:00:00Z");
        LocalDate expectedDate = LocalDate.of(2026, 7, 25);

        CheckinResultDTO result = userService.dailyCheckin(testUser.getId());
        assertTrue(result.getCheckedIn());

        assertTrue(checkinRecordRepository.existsByUserIdAndCheckinDate(testUser.getId(), expectedDate),
                "签到日期应按 Asia/Shanghai 计算为 2026-07-25");
        assertFalse(checkinRecordRepository.existsByUserIdAndCheckinDate(testUser.getId(), LocalDate.of(2026, 7, 24)),
                "UTC日期 2026-07-24 不应作为签到日期");

        // 同一上海日内重复签到应被拒绝
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(testUser.getId()));
        assertTrue(ex.getMessage().contains("已签到"));
    }

    @Test
    @DisplayName("时区边界 - 上海日界前后跨天签到")
    void dailyCheckin_timezoneBoundaryCrossMidnight() {
        // 上海 2026-07-24 23:59:59 → UTC 2026-07-24T15:59:59Z
        setFixedClock("2026-07-24T15:59:59Z");
        userService.dailyCheckin(testUser.getId());

        // 上海 2026-07-25 00:00:01 → UTC 2026-07-24T16:00:01Z（新的上海日）
        setFixedClock("2026-07-24T16:00:01Z");
        CheckinResultDTO result = userService.dailyCheckin(testUser.getId());
        assertTrue(result.getCheckedIn());
        assertEquals(7, result.getRemainingChances());
    }

    @Test
    @DisplayName("获取签到状态 - 已签到状态正确返回")
    void getCheckinStatus_checkedIn() {
        userService.dailyCheckin(testUser.getId());

        CheckinResultDTO result = userService.getCheckinStatus(testUser.getId());

        assertTrue(result.getCheckedIn());
        assertTrue(result.getMessage().contains("已签到"));
    }

    @Test
    @DisplayName("获取签到状态 - 未签到状态正确返回")
    void getCheckinStatus_notCheckedIn() {
        CheckinResultDTO result = userService.getCheckinStatus(testUser.getId());

        assertFalse(result.getCheckedIn());
        assertTrue(result.getMessage().contains("未签到"));
    }
}
