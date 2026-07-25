package com.lottery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每日签到结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignInDTO {

    /**
     * 今日是否已签到
     */
    private boolean signedInToday;

    /**
     * 最近一次签到日期
     */
    private LocalDate lastSignInDate;

    /**
     * 剩余抽奖次数
     */
    private Integer remainingChances;

    /**
     * 提示消息
     */
    private String message;
}
