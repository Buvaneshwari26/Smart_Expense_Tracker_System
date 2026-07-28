package com.tracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetDTO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    
    @JsonAlias({"limitAmount", "amount"})
    private BigDecimal budgetAmount;
    
    @JsonAlias({"budgetAmount", "amount"})
    private BigDecimal limitAmount;

    private Integer month;
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercent;
    private Boolean isExceeded;

    public BigDecimal getBudgetAmount() {
        return budgetAmount != null ? budgetAmount : limitAmount;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount != null ? limitAmount : budgetAmount;
    }
}
