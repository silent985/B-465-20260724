package com.lottery.service;

import com.lottery.dto.DrawResultDTO;
import com.lottery.dto.LotteryRecordDTO;
import com.lottery.entity.LotteryRecord;
import com.lottery.entity.Prize;
import com.lottery.entity.User;
import com.lottery.exception.BusinessException;
import com.lottery.repository.LotteryRecordRepository;
import com.lottery.repository.PrizeRepository;
import com.lottery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 抽奖服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryService {

    private final UserRepository userRepository;
    private final PrizeRepository prizeRepository;
    private final LotteryRecordRepository recordRepository;
    private final Random random = new Random();

    /**
     * 执行抽奖
     */
    @Transactional
    public DrawResultDTO draw(Long userId, String ipAddress) {
        // 1. 验证用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!user.getEnabled()) {
            throw new BusinessException("用户已被禁用");
        }

        if (user.getRemainingChances() < 1) {
            throw new BusinessException("抽奖次数不足");
        }

        // 2. 获取前端展示用的奖品列表（这个列表与前端完全一致）
        List<Prize> displayPrizes = prizeRepository.findByEnabledTrueOrderBySortOrderAsc();
        if (displayPrizes.isEmpty()) {
            throw new BusinessException("暂无可用奖品");
        }

        // 3. 筛选出有库存的奖品用于抽奖
        List<Prize> availablePrizes = displayPrizes.stream()
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());

        if (availablePrizes.isEmpty()) {
            throw new BusinessException("所有奖品已抽完");
        }

        // 4. 执行抽奖算法
        Prize prize = drawPrize(availablePrizes);

        // 5. 扣减用户抽奖次数
        int updated = userRepository.decrementChances(userId);
        if (updated == 0) {
            throw new BusinessException("抽奖次数扣减失败");
        }

        // 6. 扣减奖品库存
        if (prize.getStock() > 0) {
            prizeRepository.decrementStock(prize.getId());
        }

        // 7. 保存抽奖记录
        LotteryRecord record = LotteryRecord.builder()
                .userId(userId)
                .username(user.getUsername())
                .prizeId(prize.getId())
                .prizeName(prize.getName())
                .prizeLevel(prize.getPrizeLevel())
                .ipAddress(ipAddress)
                .build();
        recordRepository.save(record);

        // 8. 获取更新后的用户信息
        User updatedUser = userRepository.findById(userId).orElse(user);

        // 9. 计算奖品在前端展示列表中的索引（使用同一个 displayPrizes 列表）
        int prizeIndex = 0;
        for (int i = 0; i < displayPrizes.size(); i++) {
            if (displayPrizes.get(i).getId().equals(prize.getId())) {
                prizeIndex = i;
                break;
            }
        }

        log.info("用户 {} 抽中奖品: {} (等级: {})", user.getUsername(), prize.getName(), prize.getPrizeLevel());

        return DrawResultDTO.builder()
                .win(prize.getPrizeLevel() <= 4)
                .prizeId(prize.getId())
                .prizeName(prize.getName())
                .prizeDescription(prize.getDescription())
                .prizeImage(prize.getImageUrl())
                .prizeLevel(prize.getPrizeLevel())
                .prizeColor(prize.getColor())
                .prizeIndex(prizeIndex)
                .remainingChances(updatedUser.getRemainingChances())
                .drawTime(LocalDateTime.now())
                .message(prize.getPrizeLevel() <= 3 ? "恭喜您获得大奖！" : "谢谢参与")
                .build();
    }

    /**
     * 基于概率的抽奖算法
     */
    private Prize drawPrize(List<Prize> prizes) {
        // 计算总概率
        int totalProbability = prizes.stream()
                .mapToInt(Prize::getProbability)
                .sum();

        // 生成随机数
        int randomValue = random.nextInt(totalProbability);

        // 累加概率，找到中奖奖品
        int cumulativeProbability = 0;
        for (Prize prize : prizes) {
            cumulativeProbability += prize.getProbability();
            if (randomValue < cumulativeProbability) {
                return prize;
            }
        }

        // 兜底返回最后一个奖品
        return prizes.get(prizes.size() - 1);
    }

    /**
     * 获取用户抽奖记录
     */
    public Page<LotteryRecordDTO> getUserRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return recordRepository.findByUserIdOrderByDrawTimeDesc(userId, pageable)
                .map(this::toRecordDTO);
    }

    /**
     * 获取最近中奖记录（用于滚动展示）
     */
    public List<LotteryRecordDTO> getRecentWinners(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return recordRepository.findRecentWinners(pageable).stream()
                .map(this::toRecordDTO)
                .collect(Collectors.toList());
    }

    /**
     * 领取奖品
     */
    @Transactional
    public void claimPrize(Long recordId, Long userId) {
        LotteryRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("抽奖记录不存在"));

        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此记录");
        }

        if (record.getClaimed()) {
            throw new BusinessException("奖品已领取");
        }

        record.setClaimed(true);
        record.setClaimedAt(LocalDateTime.now());
        recordRepository.save(record);

        log.info("用户 {} 领取奖品: {}", record.getUsername(), record.getPrizeName());
    }

    /**
     * 获取统计数据
     */
    public LotteryStats getStats() {
        long totalDraws = recordRepository.count();
        List<LotteryRecordDTO> recentWinners = getRecentWinners(10);
        return new LotteryStats(totalDraws, recentWinners);
    }

    private LotteryRecordDTO toRecordDTO(LotteryRecord record) {
        return LotteryRecordDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .username(maskUsername(record.getUsername()))
                .prizeId(record.getPrizeId())
                .prizeName(record.getPrizeName())
                .prizeLevel(record.getPrizeLevel())
                .drawTime(record.getDrawTime())
                .claimed(record.getClaimed())
                .claimedAt(record.getClaimedAt())
                .build();
    }

    /**
     * 隐藏用户名中间部分
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }
        int len = username.length();
        return username.charAt(0) + "***" + username.charAt(len - 1);
    }

    /**
     * 统计数据内部类
     */
    public record LotteryStats(long totalDraws, List<LotteryRecordDTO> recentWinners) {}
}
