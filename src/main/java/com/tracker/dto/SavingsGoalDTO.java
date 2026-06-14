package com.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class SavingsGoalDTO {

    private Long id;

    @NotBlank(message = "Goal title is required")
    @Size(max = 100, message = "Goal title cannot exceed 100 characters")
    private String title;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be a positive number")
    private BigDecimal targetAmount;

    @NotNull(message = "Current amount is required")
    @PositiveOrZero(message = "Current amount must be zero or a positive number")
    private BigDecimal currentAmount;

    @NotNull(message = "Target date is required")
    private LocalDate targetDate;
}
