package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardSummaryDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal currentBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal monthlySavingsRate;
    private String topSpendingCategory;
    private List<BudgetProgressDTO> budgets;
    private List<RecentTransactionDTO> recentTransactions;
    private List<SavingsGoalProgressDTO> savingsGoals;
}
