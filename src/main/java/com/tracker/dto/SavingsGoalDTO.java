package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavingsGoalDTO {
    private Long id;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private LocalDate startDate;
    private String notes;
    private BigDecimal percentage;
    private BigDecimal remainingAmount;
    private String status;
    private String estimatedCompletionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
