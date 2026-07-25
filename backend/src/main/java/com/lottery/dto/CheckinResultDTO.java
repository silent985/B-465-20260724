package com.lottery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResultDTO {

    /**
     * 是否已签到
     */
    private Boolean checkedIn;

    /**
     * 剩余抽奖次数
     */
    private Integer remainingChances;

    /**
     * 奖励次数
     */
    private Integer rewardedChances;

    /**
     * 提示消息
     */
    private String message;
}
