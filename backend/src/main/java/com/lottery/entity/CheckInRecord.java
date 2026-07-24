package com.lottery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_check_in_record", indexes = {
    @Index(name = "idx_checkin_user_id", columnList = "userId"),
    @Index(name = "idx_checkin_date", columnList = "checkInDate")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_date", columnNames = {"userId", "checkInDate"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(nullable = false)
    private Long userId;

    @Size(max = 50, message = "用户名长度不能超过50")
    @Column(length = 50)
    private String username;

    @NotNull(message = "签到日期不能为空")
    @Column(nullable = false)
    private LocalDate checkInDate;

    @NotNull(message = "奖励次数不能为空")
    @Min(value = 1, message = "奖励次数最小为1")
    @Column(nullable = false)
    @Builder.Default
    private Integer rewardChances = 1;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
