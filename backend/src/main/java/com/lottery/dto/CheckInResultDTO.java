package com.lottery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResultDTO {

    private Boolean checkedIn;

    private LocalDate checkInDate;

    private Integer rewardChances;

    private Integer remainingChances;

    private String message;
}
