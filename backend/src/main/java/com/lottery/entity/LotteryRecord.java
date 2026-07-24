package com.lottery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 抽奖记录实体类
 */
@Entity
@Table(name = "t_lottery_record", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_prize_id", columnList = "prizeId"),
    @Index(name = "idx_draw_time", columnList = "drawTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 用户名（冗余字段，便于查询）
     */
    @Column(length = 50)
    private String username;

    /**
     * 奖品ID
     */
    @Column(nullable = false)
    private Long prizeId;

    /**
     * 奖品名称（冗余字段，便于查询）
     */
    @Column(nullable = false, length = 100)
    private String prizeName;

    /**
     * 奖品等级
     */
    @Column(nullable = false)
    private Integer prizeLevel;

    /**
     * 抽奖时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime drawTime;

    /**
     * IP地址
     */
    @Column(length = 50)
    private String ipAddress;

    /**
     * 是否已领取
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean claimed = false;

    /**
     * 领取时间
     */
    private LocalDateTime claimedAt;

    /**
     * 备注
     */
    @Column(length = 500)
    private String remark;
}
