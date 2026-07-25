package com.lottery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 抽奖结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawResultDTO {

    /**
     * 是否中奖
     */
    private boolean win;

    /**
     * 奖品ID
     */
    private Long prizeId;

    /**
     * 奖品名称
     */
    private String prizeName;

    /**
     * 奖品描述
     */
    private String prizeDescription;

    /**
     * 奖品图片
     */
    private String prizeImage;

    /**
     * 奖品等级
     */
    private Integer prizeLevel;

    /**
     * 奖品颜色
     */
    private String prizeColor;

    /**
     * 奖品在转盘中的索引位置
     */
    private Integer prizeIndex;

    /**
     * 剩余抽奖次数
     */
    private Integer remainingChances;

    /**
     * 抽奖时间
     */
    private LocalDateTime drawTime;

    /**
     * 提示消息
     */
    private String message;
}
