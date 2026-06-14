package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyTrendDTO {
    private int monthValue; // 1-12
    private String monthName; // e.g. "January"
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
}
