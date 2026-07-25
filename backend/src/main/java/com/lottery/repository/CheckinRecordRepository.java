package com.lottery.repository;

import com.lottery.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 签到记录数据访问层
 */
@Repository
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    /**
     * 检查用户某日是否已签到
     */
    boolean existsByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);
}
