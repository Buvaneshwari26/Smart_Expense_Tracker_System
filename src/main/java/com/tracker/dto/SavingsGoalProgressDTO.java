package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavingsGoalProgressDTO {
    private Long goalId;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal percentage;
    private LocalDate targetDate;
}
