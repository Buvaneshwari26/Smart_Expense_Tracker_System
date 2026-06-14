package com.tracker.repository;

import com.tracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    
    // Finds active budget for a category during a specific date (usually today)
    Optional<Budget> findByUserIdAndCategoryIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, Long categoryId, LocalDate date1, LocalDate date2
    );

    // Finds active budgets overlapping a specific date
    List<Budget> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate date1, LocalDate date2
    );
    
    // Checks if there's any overlapping budget for this category and user to avoid duplicate allocations
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category.id = :categoryId AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Budget> findOverlappingBudgets(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
