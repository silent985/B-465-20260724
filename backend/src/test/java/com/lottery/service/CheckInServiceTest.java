package com.lottery.service;

import com.lottery.dto.CheckInResultDTO;
import com.lottery.dto.CheckInStatusDTO;
import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.CheckInRecordRepository;
import com.lottery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckInServiceTest {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Clock clock;

    private Clock realClock;

    private User testUser;

    @BeforeEach
    void setUp() {
        checkInRecordRepository.deleteAll();
        userRepository.deleteAll();

        realClock = Clock.system(SHANGHAI_ZONE);
        when(clock.instant()).thenAnswer(inv -> realClock.instant());
        when(clock.getZone()).thenReturn(SHANGHAI_ZONE);

        testUser = User.builder()
                .username("testuser")
                .password("password")
                .nickname("Test User")
                .phone("13800000001")
                .remainingChances(10)
                .role("USER")
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    private void setFixedClock(LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(SHANGHAI_ZONE).toInstant();
        Clock fixed = Clock.fixed(instant, SHANGHAI_ZONE);
        when(clock.instant()).thenReturn(fixed.instant());
        when(clock.getZone()).thenReturn(fixed.getZone());
    }

    @Test
    @DisplayName("普通用户签到成功 - 增加1次抽奖机会并返回正确结果")
    void checkIn_Success() {
        int beforeChances = testUser.getRemainingChances();

        CheckInResultDTO result = userService.checkIn(testUser.getId());

        assertTrue(result.getCheckedIn());
        assertEquals(LocalDate.now(SHANGHAI_ZONE), result.getCheckInDate());
        assertEquals(1, result.getRewardChances());
        assertEquals(beforeChances + 1, result.getRemainingChances());
        assertTrue(result.getMessage().contains("签到成功"));

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(beforeChances + 1, updatedUser.getRemainingChances());

        assertTrue(checkInRecordRepository.existsByUserIdAndCheckInDate(
                testUser.getId(), LocalDate.now(SHANGHAI_ZONE)));
    }

    @Test
    @DisplayName("重复签到 - 今日已签到时抛出业务异常")
    void checkIn_Duplicate_ThrowsException() {
        userService.checkIn(testUser.getId());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.checkIn(testUser.getId());
        });

        assertTrue(exception.getMessage().contains("今日已签到"));
    }

    @Test
    @DisplayName("签到 - 用户不存在时抛出业务异常")
    void checkIn_UserNotFound_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.checkIn(99999L);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    @DisplayName("签到 - 用户被禁用时抛出业务异常")
    void checkIn_UserDisabled_ThrowsException() {
        User disabledUser = userRepository.save(User.builder()
                .username("disableduser")
                .password("password")
                .nickname("Disabled User")
                .remainingChances(10)
                .role("USER")
                .enabled(false)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.checkIn(disabledUser.getId());
        });

        assertTrue(exception.getMessage().contains("用户已被禁用"));
    }

    @Test
    @DisplayName("管理员签到被拒绝 - role为ADMIN时抛出异常")
    void checkIn_Admin_ThrowsException() {
        User adminUser = userRepository.save(User.builder()
                .username("checkinadmin")
                .password("password")
                .nickname("CheckIn Admin")
                .remainingChances(999)
                .role("ADMIN")
                .enabled(true)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.checkIn(adminUser.getId());
        });

        assertTrue(exception.getMessage().contains("仅普通用户可签到"));
    }

    @Test
    @DisplayName("异常角色签到被拒绝 - role为GUEST时抛出异常")
    void checkIn_AbnormalRole_ThrowsException() {
        User guestUser = userRepository.save(User.builder()
                .username("guestuser")
                .password("password")
                .nickname("Guest User")
                .remainingChances(10)
                .role("GUEST")
                .enabled(true)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.checkIn(guestUser.getId());
        });

        assertTrue(exception.getMessage().contains("仅普通用户可签到"));
    }

    @Test
    @DisplayName("空角色签到被拒绝 - role为空字符串时抛出异常")
    void checkIn_EmptyRole_ThrowsException() {
        User emptyRoleUser = userRepository.save(User.builder()
                .username("noroleuser")
                .password("password")
                .nickname("No Role User")
                .remainingChances(10)
                .role("")
                .enabled(true)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.checkIn(emptyRoleUser.getId());
        });

        assertTrue(exception.getMessage().contains("仅普通用户可签到"));
    }

    @Test
    @DisplayName("管理员获取签到状态被拒绝")
    void getCheckInStatus_Admin_ThrowsException() {
        User adminUser = userRepository.save(User.builder()
                .username("statusadmin")
                .password("password")
                .nickname("Status Admin")
                .remainingChances(999)
                .role("ADMIN")
                .enabled(true)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getCheckInStatus(adminUser.getId());
        });

        assertTrue(exception.getMessage().contains("仅普通用户可签到"));
    }

    @Test
    @DisplayName("异常角色获取签到状态被拒绝")
    void getCheckInStatus_AbnormalRole_ThrowsException() {
        User guestUser = userRepository.save(User.builder()
                .username("gueststatus")
                .password("password")
                .nickname("Guest Status")
                .remainingChances(10)
                .role("GUEST")
                .enabled(true)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getCheckInStatus(guestUser.getId());
        });

        assertTrue(exception.getMessage().contains("仅普通用户可签到"));
    }

    @Test
    @DisplayName("并发签到 - 多线程并发签到只有一次成功")
    void checkIn_Concurrent_OnlyOneSucceeds() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    txTemplate.execute(status -> {
                        try {
                            userService.checkIn(testUser.getId());
                            successCount.incrementAndGet();
                        } catch (BusinessException e) {
                            if (e.getMessage().contains("今日已签到")) {
                                duplicateCount.incrementAndGet();
                            }
                        }
                        return null;
                    });
                } catch (Exception e) {
                    if (e.getCause() instanceof BusinessException) {
                        BusinessException be = (BusinessException) e.getCause();
                        if (be.getMessage().contains("今日已签到")) {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one concurrent check-in should succeed");
        assertEquals(threadCount - 1, duplicateCount.get(), "Others should get duplicate message");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(testUser.getRemainingChances() + 1, updatedUser.getRemainingChances(),
                "Chances should be incremented exactly once");

        long recordCount = checkInRecordRepository.count();
        assertEquals(1, recordCount, "Only one check-in record should exist");
    }

    @Test
    @DisplayName("签到与充值并发 - 剩余次数准确性")
    void checkIn_ConcurrentWithAddChances_AccurateRemainingChances() throws InterruptedException {
        int initialChances = testUser.getRemainingChances();
        int addCount = 5;
        int threadCount = 2;

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        executor.submit(() -> {
            try {
                barrier.await();
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                txTemplate.execute(status -> {
                    userService.checkIn(testUser.getId());
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                barrier.await();
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                txTemplate.execute(status -> {
                    userService.addChances(testUser.getId(), addCount);
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        });

        endLatch.await();
        executor.shutdown();

        User finalUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(initialChances + 1 + addCount, finalUser.getRemainingChances(),
                "Remaining chances should be initial + 1 (check-in) + " + addCount + " (addChances)");
    }

    @Test
    @DisplayName("签到后剩余次数与DB实际值一致")
    void checkIn_ReturnsActualDbValue() {
        CheckInResultDTO result = userService.checkIn(testUser.getId());

        User dbUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(dbUser.getRemainingChances(), result.getRemainingChances(),
                "Returned remaining chances should match DB value");
    }

    @Test
    @DisplayName("Asia/Shanghai时区 - 跨零点边界测试（23:59签到，00:01次日可再次签到）")
    void checkIn_ShanghaiTimezone_CrossMidnightBoundary() {
        LocalDateTime beforeMidnight = LocalDate.now(SHANGHAI_ZONE).atTime(23, 59, 50);
        setFixedClock(beforeMidnight);

        CheckInResultDTO result1 = userService.checkIn(testUser.getId());
        assertTrue(result1.getCheckedIn());
        assertEquals(beforeMidnight.toLocalDate(), result1.getCheckInDate());

        BusinessException duplicateException = assertThrows(BusinessException.class, () -> {
            userService.checkIn(testUser.getId());
        });
        assertTrue(duplicateException.getMessage().contains("今日已签到"));

        LocalDateTime afterMidnight = beforeMidnight.plusMinutes(2);
        setFixedClock(afterMidnight);

        CheckInResultDTO result2 = userService.checkIn(testUser.getId());
        assertTrue(result2.getCheckedIn());
        assertEquals(afterMidnight.toLocalDate(), result2.getCheckInDate());

        long recordCount = checkInRecordRepository.count();
        assertEquals(2, recordCount, "Should have 2 check-in records across 2 days");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(testUser.getRemainingChances() + 2, updatedUser.getRemainingChances());
    }

    @Test
    @DisplayName("获取签到状态 - 未签到时返回false")
    void getCheckInStatus_NotCheckedIn() {
        CheckInStatusDTO status = userService.getCheckInStatus(testUser.getId());

        assertFalse(status.getCheckedInToday());
        assertEquals(LocalDate.now(SHANGHAI_ZONE), status.getToday());
        assertEquals(testUser.getRemainingChances(), status.getRemainingChances());
    }

    @Test
    @DisplayName("获取签到状态 - 签到后返回true")
    void getCheckInStatus_AfterCheckIn() {
        userService.checkIn(testUser.getId());

        CheckInStatusDTO status = userService.getCheckInStatus(testUser.getId());

        assertTrue(status.getCheckedInToday());
        assertEquals(LocalDate.now(SHANGHAI_ZONE), status.getToday());
    }

    @Test
    @DisplayName("获取签到状态 - 用户不存在时抛出异常")
    void getCheckInStatus_UserNotFound_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getCheckInStatus(99999L);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    @DisplayName("获取签到状态 - 用户被禁用时抛出异常")
    void getCheckInStatus_UserDisabled_ThrowsException() {
        User disabledUser = userRepository.save(User.builder()
                .username("disableduser2")
                .password("password")
                .nickname("Disabled User 2")
                .remainingChances(10)
                .role("USER")
                .enabled(false)
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getCheckInStatus(disabledUser.getId());
        });

        assertTrue(exception.getMessage().contains("用户已被禁用"));
    }

    @Test
    @DisplayName("Controller参数校验 - 用户ID小于1时返回400")
    void controller_InvalidId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/0/check-in")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users/0/check-in/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Controller参数校验 - 有效用户ID正常处理")
    void controller_ValidId_ProcessesRequest() throws Exception {
        mockMvc.perform(post("/api/users/" + testUser.getId() + "/check-in")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.checkedIn").value(true))
                .andExpect(jsonPath("$.data.remainingChances").value(11));
    }
}
