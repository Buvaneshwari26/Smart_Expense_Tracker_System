package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetDTO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal budgetAmount;
    private Integer month;
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercent;
    private Boolean isExceeded;
}
