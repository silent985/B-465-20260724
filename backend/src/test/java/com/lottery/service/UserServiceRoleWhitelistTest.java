package com.lottery.service;

import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.CheckinRecordRepository;
import com.lottery.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceRoleWhitelistTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CheckinRecordRepository checkinRecordRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("白名单校验 - role为null时拒绝签到")
    void dailyCheckin_nullRoleRejected() {
        User user = User.builder()
                .id(1L)
                .username("nullrole")
                .password("pw")
                .remainingChances(5)
                .role(null)
                .enabled(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(1L));
        assertTrue(ex.getMessage().contains("普通用户"));

        verify(checkinRecordRepository, never()).save(any());
        verify(userRepository, never()).incrementChances(anyLong(), anyInt());
    }

    @Test
    @DisplayName("白名单校验 - 空字符串角色拒绝签到")
    void dailyCheckin_emptyRoleRejected() {
        User user = User.builder()
                .id(2L)
                .username("emptyrole")
                .password("pw")
                .remainingChances(5)
                .role("")
                .enabled(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(2L));
        assertTrue(ex.getMessage().contains("普通用户"));

        verify(checkinRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("白名单校验 - 任意非USER角色拒绝签到")
    void dailyCheckin_otherRoleRejected() {
        User user = User.builder()
                .id(3L)
                .username("guest")
                .password("pw")
                .remainingChances(5)
                .role("GUEST")
                .enabled(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.dailyCheckin(3L));
        assertTrue(ex.getMessage().contains("普通用户"));
    }
}
