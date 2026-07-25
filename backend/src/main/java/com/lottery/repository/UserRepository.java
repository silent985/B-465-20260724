package com.lottery.repository;

import com.lottery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 扣减抽奖次数
     */
    @Modifying
    @Query("UPDATE User u SET u.remainingChances = u.remainingChances - 1 WHERE u.id = :userId AND u.remainingChances > 0")
    int decrementChances(@Param("userId") Long userId);

    /**
     * 增加抽奖次数
     */
    @Modifying
    @Query("UPDATE User u SET u.remainingChances = u.remainingChances + :count WHERE u.id = :userId")
    int incrementChances(@Param("userId") Long userId, @Param("count") int count);

    /**
     * 每日签到（原子操作）：仅当用户为启用状态的普通用户（USER）且今日未签到时，
     * 将签到日期置为当天并增加1次抽奖机会。
     * 返回受影响行数，0 表示不满足签到条件（不存在/被禁用/非普通用户/今日已签到），
     * 保证并发场景下每天只签到成功一次。
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.remainingChances = u.remainingChances + 1, u.lastSignInDate = :today "
            + "WHERE u.id = :userId AND u.enabled = true AND u.role = 'USER' "
            + "AND (u.lastSignInDate IS NULL OR u.lastSignInDate < :today)")
    int signInToday(@Param("userId") Long userId, @Param("today") LocalDate today);
}
