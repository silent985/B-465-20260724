package com.lottery.service;

import com.lottery.dto.SignInDTO;
import com.lottery.dto.UserDTO;
import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 普通用户角色
     */
    private static final String ROLE_USER = "USER";

    private final UserRepository userRepository;
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
     * 每日签到领取抽奖次数：仅允许启用状态的普通用户（USER）签到，每天仅可签到一次，
     * 签到成功增加1次抽奖机会。角色、启用状态、当天未签到与次数自增在同一条原子更新中完成，
     * 保证并发安全；签到日期基于业务时区（Asia/Shanghai）的当天。
     */
    @Transactional
    public SignInDTO signIn(Long userId) {
        LocalDate today = LocalDate.now(clock);

        // 角色、启用状态、当天未签到、次数自增合并为同一条原子更新
        int updated = userRepository.signInToday(userId, today);
        if (updated == 0) {
            // 更新未命中：加载用户以区分具体原因（不存在/被禁用/非普通用户/今日已签到）
            throw signInFailure(userId);
        }

        User signedUser = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        log.info("用户 {} 签到成功，剩余抽奖次数: {}", signedUser.getUsername(), signedUser.getRemainingChances());

        return SignInDTO.builder()
                .signedInToday(true)
                .lastSignInDate(signedUser.getLastSignInDate())
                .remainingChances(signedUser.getRemainingChances())
                .message("签到成功，获得1次抽奖机会")
                .build();
    }

    /**
     * 根据当前用户状态判定签到失败的具体原因
     */
    private BusinessException signInFailure(Long userId) {
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return new BusinessException("用户不存在");
        }
        if (!user.getEnabled()) {
            return new BusinessException("用户已被禁用");
        }
        if (!ROLE_USER.equals(user.getRole())) {
            return new BusinessException("仅普通用户可参与签到");
        }
        return new BusinessException("今日已签到，请明天再来");
    }

    /**
     * 获取用户今日签到状态
     */
    public SignInDTO getSignInStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        boolean signedInToday = LocalDate.now(clock).equals(user.getLastSignInDate());
        return SignInDTO.builder()
                .signedInToday(signedInToday)
                .lastSignInDate(user.getLastSignInDate())
                .remainingChances(user.getRemainingChances())
                .message(signedInToday ? "今日已签到" : "今日未签到")
                .build();
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
