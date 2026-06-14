package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private List<BudgetProgressDTO> budgets;
    private List<RecentTransactionDTO> recentTransactions;
    private List<SavingsGoalProgressDTO> savingsGoals;
}
