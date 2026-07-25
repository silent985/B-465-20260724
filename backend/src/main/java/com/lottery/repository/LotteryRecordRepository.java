package com.lottery.repository;

import com.lottery.entity.LotteryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 抽奖记录数据访问层
 */
@Repository
public interface LotteryRecordRepository extends JpaRepository<LotteryRecord, Long> {

    /**
     * 根据用户ID查询抽奖记录（分页）
     */
    Page<LotteryRecord> findByUserIdOrderByDrawTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID查询所有抽奖记录
     */
    List<LotteryRecord> findByUserIdOrderByDrawTimeDesc(Long userId);

    /**
     * 查询最近的中奖记录
     */
    @Query("SELECT r FROM LotteryRecord r WHERE r.prizeLevel <= 3 ORDER BY r.drawTime DESC")
    List<LotteryRecord> findRecentWinners(Pageable pageable);

    /**
     * 查询用户今日抽奖次数
     */
    @Query("SELECT COUNT(r) FROM LotteryRecord r WHERE r.userId = :userId AND r.drawTime >= :startTime")
    long countTodayDraws(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 统计总抽奖次数
     */
    long count();

    /**
     * 统计某奖品的中奖次数
     */
    long countByPrizeId(Long prizeId);
}
