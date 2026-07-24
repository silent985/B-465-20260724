package com.lottery.repository;

import com.lottery.entity.CheckInRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {

    Optional<CheckInRecord> findByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    boolean existsByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);
}
