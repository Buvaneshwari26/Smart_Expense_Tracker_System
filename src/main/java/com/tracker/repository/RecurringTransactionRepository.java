package com.tracker.repository;

import com.tracker.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUserId(Long userId);
    List<RecurringTransaction> findByIsActiveTrueAndNextExecutionDateLessThanEqual(LocalDate date);
}
