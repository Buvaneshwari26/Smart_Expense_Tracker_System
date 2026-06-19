package com.tracker.repository;

import com.tracker.model.Income;
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
public interface IncomeRepository extends JpaRepository<Income, Long> {
    Page<Income> findByUserId(Long userId, Pageable pageable);
    List<Income> findByUserId(Long userId);
    Optional<Income> findByIdAndUserId(Long id, Long userId);
    List<Income> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId AND MONTH(i.date) = :month AND YEAR(i.date) = :year")
    BigDecimal sumByUserIdAndMonthAndYear(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId")
    BigDecimal sumTotalByUserId(@Param("userId") Long userId);
}
