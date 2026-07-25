package com.lottery.service;

import com.lottery.dto.CheckinResultDTO;
import com.lottery.dto.UserDTO;
import com.lottery.entity.CheckinRecord;
import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.CheckinRecordRepository;
import com.lottery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final Clock clock;

    /**
     * 用户登录（简化版，实际应使用Spring Security）
     */
    public UserDTO login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 简化密码验证（实际应使用BCrypt）
        if (!user.getPassword().equals(password)) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!user.getEnabled()) {
            throw new BusinessException("用户已被禁用");
        }

        log.info("用户登录: {}", username);
        return toDTO(user);
    }

    /**
     * 用户注册
     */
    @Transactional
    public UserDTO register(String username, String password, String nickname, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已被注册");
        }

        User user = User.builder()
                .username(username)
                .password(password) // 实际应使用BCrypt加密
                .nickname(nickname != null ? nickname : username)
                .phone(phone)
                .remainingChances(10)
                .role("USER")
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("用户注册: {}", username);
        return toDTO(saved);
    }

    /**
     * 获取用户信息
     */
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toDTO(user);
    }

    /**
     * 获取用户信息（通过用户名）
     */
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toDTO(user);
    }

    /**
     * 获取所有用户（分页）
     */
    public Page<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable).map(this::toDTO);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }

        User saved = userRepository.save(user);
        log.info("更新用户信息: {}", user.getUsername());
        return toDTO(saved);
    }

    /**
     * 增加用户抽奖次数
     */
    @Transactional
    public UserDTO addChances(Long userId, int count) {
        if (count <= 0) {
            throw new BusinessException("增加次数必须大于0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        userRepository.incrementChances(userId, count);

        User updated = userRepository.findById(userId).orElse(user);
        log.info("增加用户 {} 抽奖次数: +{}", user.getUsername(), count);
        return toDTO(updated);
    }

    /**
     * 切换用户启用状态
     */
    @Transactional
    public UserDTO toggleEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setEnabled(!user.getEnabled());
        User saved = userRepository.save(user);
        log.info("切换用户状态: {} -> {}", user.getUsername(), saved.getEnabled() ? "启用" : "禁用");
        return toDTO(saved);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("用户不存在");
        }
        userRepository.deleteById(id);
        log.info("删除用户: ID {}", id);
    }

    /**
     * 每日签到
     */
    @Transactional
    public CheckinResultDTO dailyCheckin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!user.getEnabled()) {
            throw new BusinessException("用户已被禁用");
        }

        if (!"USER".equals(user.getRole())) {
            throw new BusinessException("仅普通用户可签到");
        }

        LocalDate today = LocalDate.now(clock);
        if (checkinRecordRepository.existsByUserIdAndCheckinDate(userId, today)) {
            throw new BusinessException("今日已签到，请勿重复签到");
        }

        CheckinRecord record = CheckinRecord.builder()
                .userId(userId)
                .checkinDate(today)
                .chancesRewarded(1)
                .build();

        try {
            checkinRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("今日已签到，请勿重复签到");
        }

        userRepository.incrementChances(userId, 1);

        User updated = userRepository.findById(userId).orElseThrow();
        log.info("用户 {} 每日签到成功，获得1次抽奖机会", user.getUsername());

        return CheckinResultDTO.builder()
                .checkedIn(true)
                .remainingChances(updated.getRemainingChances())
                .rewardedChances(1)
                .message("签到成功，获得1次抽奖机会")
                .build();
    }

    /**
     * 获取签到状态
     */
    public CheckinResultDTO getCheckinStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LocalDate today = LocalDate.now(clock);
        boolean checkedIn = checkinRecordRepository.existsByUserIdAndCheckinDate(userId, today);

        return CheckinResultDTO.builder()
                .checkedIn(checkedIn)
                .remainingChances(user.getRemainingChances())
                .rewardedChances(0)
                .message(checkedIn ? "今日已签到" : "今日未签到")
                .build();
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .remainingChances(user.getRemainingChances())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();
    }
}
