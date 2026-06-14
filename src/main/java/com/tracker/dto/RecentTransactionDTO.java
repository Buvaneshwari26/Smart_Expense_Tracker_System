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
public class RecentTransactionDTO {
    private Long id;
    private String type; // "INCOME" or "EXPENSE"
    private BigDecimal amount;
    private LocalDate date;
    private String categoryName;
    private String description;
}
