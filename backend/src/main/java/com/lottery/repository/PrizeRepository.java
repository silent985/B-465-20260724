package com.lottery.repository;

import com.lottery.entity.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 奖品数据访问层
 */
@Repository
public interface PrizeRepository extends JpaRepository<Prize, Long> {

    /**
     * 查询所有启用的奖品（按排序顺序）
     */
    List<Prize> findByEnabledTrueOrderBySortOrderAsc();

    /**
     * 查询所有有库存的启用奖品
     */
    @Query("SELECT p FROM Prize p WHERE p.enabled = true AND p.stock > 0 ORDER BY p.sortOrder ASC")
    List<Prize> findAvailablePrizes();

    /**
     * 扣减库存
     */
    @Modifying
    @Query("UPDATE Prize p SET p.stock = p.stock - 1, p.drawnCount = p.drawnCount + 1 WHERE p.id = :prizeId AND p.stock > 0")
    int decrementStock(@Param("prizeId") Long prizeId);

    /**
     * 根据等级查询奖品
     */
    List<Prize> findByPrizeLevelAndEnabledTrueOrderBySortOrderAsc(Integer prizeLevel);
}
