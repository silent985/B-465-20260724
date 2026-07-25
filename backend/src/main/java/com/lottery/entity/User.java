package com.lottery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Entity
@Table(name = "t_user", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_phone", columnList = "phone")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（加密存储）
     */
    @NotBlank(message = "密码不能为空")
    @Column(nullable = false, length = 100)
    private String password;

    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称长度不能超过50")
    @Column(length = 50)
    private String nickname;

    /**
     * 手机号
     */
    @Size(max = 20, message = "手机号长度不能超过20")
    @Column(length = 20)
    private String phone;

    /**
     * 剩余抽奖次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer remainingChances = 10;

    /**
     * 最近一次签到日期（用于每日签到领取抽奖次数）
     */
    @Column
    private LocalDate lastSignInDate;

    /**
     * 用户角色: ADMIN, USER
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    /**
     * 是否启用
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
