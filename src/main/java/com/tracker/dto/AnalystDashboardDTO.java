package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalystDashboardDTO {
    private List<MonthlyTrend> monthlyTrends;
    private List<CategorySpending> categoryAnalysis;
    private List<String> financialInsights;
    private BigDecimal averageMonthlyIncome;
    private BigDecimal averageMonthlyExpense;
    private BigDecimal savingsRate;
    private BigDecimal budgetAdherenceRate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyTrend {
        private String month;
        private BigDecimal income;
        private BigDecimal expense;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySpending {
        private String categoryName;
        private BigDecimal totalSpent;
        private BigDecimal percentage;
    }
}
