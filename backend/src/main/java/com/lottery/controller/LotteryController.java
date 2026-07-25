package com.lottery.controller;

import com.lottery.dto.ApiResponse;
import com.lottery.dto.DrawResultDTO;
import com.lottery.dto.LotteryRecordDTO;
import com.lottery.service.LotteryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 抽奖控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
@Tag(name = "抽奖接口", description = "抽奖相关的API接口")
public class LotteryController {

    private final LotteryService lotteryService;

    @PostMapping("/draw/{userId}")
    @Operation(summary = "执行抽奖", description = "用户进行一次抽奖，需要消耗抽奖次数")
    public ApiResponse<DrawResultDTO> draw(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        DrawResultDTO result = lotteryService.draw(userId, ipAddress);
        return ApiResponse.success("抽奖成功", result);
    }

    @GetMapping("/records/{userId}")
    @Operation(summary = "获取用户抽奖记录", description = "分页获取用户的抽奖记录")
    public ApiResponse<Page<LotteryRecordDTO>> getUserRecords(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Page<LotteryRecordDTO> records = lotteryService.getUserRecords(userId, page, size);
        return ApiResponse.success(records);
    }

    @GetMapping("/recent-winners")
    @Operation(summary = "获取最近中奖记录", description = "获取最近的中奖记录，用于滚动展示")
    public ApiResponse<List<LotteryRecordDTO>> getRecentWinners(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        List<LotteryRecordDTO> winners = lotteryService.getRecentWinners(limit);
        return ApiResponse.success(winners);
    }

    @PostMapping("/claim/{recordId}")
    @Operation(summary = "领取奖品", description = "用户领取已中奖的奖品")
    public ApiResponse<Void> claimPrize(
            @Parameter(description = "抽奖记录ID") @PathVariable Long recordId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        lotteryService.claimPrize(recordId, userId);
        return ApiResponse.success("领取成功", null);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取抽奖统计", description = "获取抽奖统计数据")
    public ApiResponse<LotteryService.LotteryStats> getStats() {
        return ApiResponse.success(lotteryService.getStats());
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
