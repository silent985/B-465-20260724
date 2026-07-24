package com.lottery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到记录实体类
 */
@Entity
@Table(name = "t_checkin_record", uniqueConstraints = {
    @UniqueConstraint(name = "uk_checkin_user_date", columnNames = {"userId", "checkinDate"})
}, indexes = {
    @Index(name = "idx_checkin_user_id", columnList = "userId"),
    @Index(name = "idx_checkin_date", columnList = "checkinDate")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 签到日期
     */
    @Column(nullable = false)
    private LocalDate checkinDate;

    /**
     * 奖励抽奖次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer chancesRewarded = 1;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
