package com.tracker.repository;

import com.tracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    Optional<Expense> findByIdAndUserId(Long id, Long userId);
    List<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId);
    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Expense> findByUserIdAndCategoryIdAndDateBetween(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate);
    List<Expense> findTop5ByUserIdOrderByDateDesc(Long userId);
}
