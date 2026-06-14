package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalProgressDTO {
    private Long goalId;
    private String title;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal percentage; // e.g. 75.5%
    private LocalDate targetDate;
}
