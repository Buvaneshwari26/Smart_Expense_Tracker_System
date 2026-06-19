package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetProgressDTO {
    private Long budgetId;
    private String categoryName;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercent;
    private boolean isExceeded;
}
