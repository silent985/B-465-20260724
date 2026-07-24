package com.lottery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 抽奖记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryRecordDTO {

    private Long id;

    private Long userId;

    private String username;

    private Long prizeId;

    private String prizeName;

    private Integer prizeLevel;

    private LocalDateTime drawTime;

    private Boolean claimed;

    private LocalDateTime claimedAt;
}
