package com.tracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ExpenseDTO {

    private Long id;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive number")
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String categoryName; // Read-only for responses
}
