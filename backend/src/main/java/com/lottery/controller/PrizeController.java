package com.lottery.controller;

import com.lottery.dto.ApiResponse;
import com.lottery.dto.PrizeDTO;
import com.lottery.service.PrizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 奖品控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/prizes")
@RequiredArgsConstructor
@Tag(name = "奖品接口", description = "奖品管理相关的API接口")
public class PrizeController {

    private final PrizeService prizeService;

    @GetMapping
    @Operation(summary = "获取所有奖品", description = "获取所有奖品列表（管理员用）")
    public ApiResponse<List<PrizeDTO>> getAllPrizes() {
        List<PrizeDTO> prizes = prizeService.getAllPrizes();
        return ApiResponse.success(prizes);
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取启用的奖品", description = "获取所有启用的奖品列表（抽奖展示用）")
    public ApiResponse<List<PrizeDTO>> getEnabledPrizes() {
        List<PrizeDTO> prizes = prizeService.getEnabledPrizes();
        return ApiResponse.success(prizes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取奖品详情", description = "根据ID获取奖品详情")
    public ApiResponse<PrizeDTO> getPrizeById(
            @Parameter(description = "奖品ID") @PathVariable Long id) {
        PrizeDTO prize = prizeService.getPrizeById(id);
        return ApiResponse.success(prize);
    }

    @PostMapping
    @Operation(summary = "创建奖品", description = "创建新的奖品")
    public ApiResponse<PrizeDTO> createPrize(
            @Valid @RequestBody PrizeDTO prizeDTO) {
        PrizeDTO created = prizeService.createPrize(prizeDTO);
        return ApiResponse.success("创建成功", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新奖品", description = "更新奖品信息")
    public ApiResponse<PrizeDTO> updatePrize(
            @Parameter(description = "奖品ID") @PathVariable Long id,
            @Valid @RequestBody PrizeDTO prizeDTO) {
        PrizeDTO updated = prizeService.updatePrize(id, prizeDTO);
        return ApiResponse.success("更新成功", updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除奖品", description = "删除指定的奖品")
    public ApiResponse<Void> deletePrize(
            @Parameter(description = "奖品ID") @PathVariable Long id) {
        prizeService.deletePrize(id);
        return ApiResponse.success("删除成功", null);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "切换奖品状态", description = "切换奖品的启用/禁用状态")
    public ApiResponse<PrizeDTO> toggleEnabled(
            @Parameter(description = "奖品ID") @PathVariable Long id) {
        PrizeDTO prize = prizeService.toggleEnabled(id);
        return ApiResponse.success("状态切换成功", prize);
    }
}
