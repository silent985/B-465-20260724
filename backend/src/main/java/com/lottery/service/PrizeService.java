package com.lottery.service;

import com.lottery.dto.PrizeDTO;
import com.lottery.entity.Prize;
import com.lottery.exception.BusinessException;
import com.lottery.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 奖品服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrizeService {

    private final PrizeRepository prizeRepository;

    /**
     * 获取所有奖品
     */
    public List<PrizeDTO> getAllPrizes() {
        return prizeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取启用的奖品（用于抽奖展示）
     */
    public List<PrizeDTO> getEnabledPrizes() {
        return prizeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取奖品
     */
    public PrizeDTO getPrizeById(Long id) {
        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("奖品不存在"));
        return toDTO(prize);
    }

    /**
     * 创建奖品
     */
    @Transactional
    public PrizeDTO createPrize(PrizeDTO dto) {
        Prize prize = Prize.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .probability(dto.getProbability() != null ? dto.getProbability() : 100)
                .stock(dto.getStock() != null ? dto.getStock() : 100)
                .prizeLevel(dto.getPrizeLevel() != null ? dto.getPrizeLevel() : 5)
                .color(dto.getColor() != null ? dto.getColor() : "#FF6B6B")
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();

        Prize saved = prizeRepository.save(prize);
        log.info("创建奖品: {} (ID: {})", saved.getName(), saved.getId());
        return toDTO(saved);
    }

    /**
     * 更新奖品
     */
    @Transactional
    public PrizeDTO updatePrize(Long id, PrizeDTO dto) {
        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("奖品不存在"));

        if (dto.getName() != null) {
            prize.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            prize.setDescription(dto.getDescription());
        }
        if (dto.getImageUrl() != null) {
            prize.setImageUrl(dto.getImageUrl());
        }
        if (dto.getProbability() != null) {
            prize.setProbability(dto.getProbability());
        }
        if (dto.getStock() != null) {
            prize.setStock(dto.getStock());
        }
        if (dto.getPrizeLevel() != null) {
            prize.setPrizeLevel(dto.getPrizeLevel());
        }
        if (dto.getColor() != null) {
            prize.setColor(dto.getColor());
        }
        if (dto.getEnabled() != null) {
            prize.setEnabled(dto.getEnabled());
        }
        if (dto.getSortOrder() != null) {
            prize.setSortOrder(dto.getSortOrder());
        }

        Prize saved = prizeRepository.save(prize);
        log.info("更新奖品: {} (ID: {})", saved.getName(), saved.getId());
        return toDTO(saved);
    }

    /**
     * 删除奖品
     */
    @Transactional
    public void deletePrize(Long id) {
        if (!prizeRepository.existsById(id)) {
            throw new BusinessException("奖品不存在");
        }
        prizeRepository.deleteById(id);
        log.info("删除奖品: ID {}", id);
    }

    /**
     * 切换奖品启用状态
     */
    @Transactional
    public PrizeDTO toggleEnabled(Long id) {
        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("奖品不存在"));
        prize.setEnabled(!prize.getEnabled());
        Prize saved = prizeRepository.save(prize);
        log.info("切换奖品状态: {} -> {}", saved.getName(), saved.getEnabled() ? "启用" : "禁用");
        return toDTO(saved);
    }

    private PrizeDTO toDTO(Prize prize) {
        return PrizeDTO.builder()
                .id(prize.getId())
                .name(prize.getName())
                .description(prize.getDescription())
                .imageUrl(prize.getImageUrl())
                .probability(prize.getProbability())
                .stock(prize.getStock())
                .drawnCount(prize.getDrawnCount())
                .prizeLevel(prize.getPrizeLevel())
                .color(prize.getColor())
                .enabled(prize.getEnabled())
                .sortOrder(prize.getSortOrder())
                .build();
    }
}
