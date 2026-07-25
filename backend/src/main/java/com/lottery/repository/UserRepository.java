package com.lottery.repository;

import com.lottery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.remainingChances = u.remainingChances + :count WHERE u.id = :userId")
    int incrementChances(@Param("userId") Long userId, @Param("count") int count);
}
