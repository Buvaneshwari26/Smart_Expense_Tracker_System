package com.tracker.repository;

import com.tracker.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByUserId(Long userId, Pageable pageable);
    List<Expense> findByUserId(Long userId);
    Optional<Expense> findByIdAndUserId(Long id, Long userId);
    Page<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);
    Page<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Expense> findTop5ByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    BigDecimal sumByUserIdAndMonthAndYear(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND MONTH(e.date) = :month AND YEAR(e.date) = :year GROUP BY e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> findCategoryWiseTotals(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
           "AND (:keyword IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
           "AND (:startDate IS NULL OR e.date >= :startDate) " +
           "AND (:endDate IS NULL OR e.date <= :endDate)")
    Page<Expense> searchExpenses(@Param("userId") Long userId,
                                  @Param("keyword") String keyword,
                                  @Param("categoryId") Long categoryId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  Pageable pageable);
}
