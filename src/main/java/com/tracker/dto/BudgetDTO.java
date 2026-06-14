package com.tracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class BudgetDTO {

    private Long id;

    @NotNull(message = "Budget amount is required")
    @Positive(message = "Budget amount must be a positive number")
    private BigDecimal amount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String categoryName; // Read-only for responses
}
