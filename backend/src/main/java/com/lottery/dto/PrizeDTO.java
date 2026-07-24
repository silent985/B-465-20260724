package com.lottery.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 奖品DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrizeDTO {

    private Long id;

    @NotBlank(message = "奖品名称不能为空")
    @Size(max = 100, message = "奖品名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "奖品描述长度不能超过500")
    private String description;

    @Size(max = 500, message = "图片URL长度不能超过500")
    private String imageUrl;

    @Min(value = 1, message = "中奖概率最小为1")
    @Max(value = 1000, message = "中奖概率最大为1000")
    private Integer probability;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private Integer drawnCount;

    @Min(value = 1, message = "奖品等级最小为1")
    @Max(value = 5, message = "奖品等级最大为5")
    private Integer prizeLevel;

    private String color;

    private Boolean enabled;

    private Integer sortOrder;
}
