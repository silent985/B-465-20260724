package com.lottery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 奖品实体类
 */
@Entity
@Table(name = "t_prize", indexes = {
    @Index(name = "idx_enabled", columnList = "enabled"),
    @Index(name = "idx_prize_level", columnList = "prizeLevel")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 奖品名称
     */
    @NotBlank(message = "奖品名称不能为空")
    @Size(max = 100, message = "奖品名称长度不能超过100")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 奖品描述
     */
    @Size(max = 500, message = "奖品描述长度不能超过500")
    @Column(length = 500)
    private String description;

    /**
     * 奖品图片URL
     */
    @Size(max = 500, message = "图片URL长度不能超过500")
    @Column(length = 500)
    private String imageUrl;

    /**
     * 中奖概率（千分比，1-1000）
     */
    @Min(value = 1, message = "中奖概率最小为1")
    @Max(value = 1000, message = "中奖概率最大为1000")
    @Column(nullable = false)
    @Builder.Default
    private Integer probability = 100;

    /**
     * 库存数量
     */
    @Min(value = 0, message = "库存不能为负数")
    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 100;

    /**
     * 已抽取数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer drawnCount = 0;

    /**
     * 奖品等级（1-特等奖, 2-一等奖, 3-二等奖, 4-三等奖, 5-参与奖）
     */
    @Min(value = 1, message = "奖品等级最小为1")
    @Max(value = 5, message = "奖品等级最大为5")
    @Column(nullable = false)
    @Builder.Default
    private Integer prizeLevel = 5;

    /**
     * 奖品颜色（用于转盘显示）
     */
    @Column(length = 20)
    @Builder.Default
    private String color = "#FF6B6B";

    /**
     * 是否启用
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 排序顺序
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

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

    /**
     * 检查是否有库存
     */
    public boolean hasStock() {
        return this.stock > 0;
    }
}
