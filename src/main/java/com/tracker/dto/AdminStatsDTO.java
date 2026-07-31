package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO returned by GET /api/admin/stats.
 * Carries system-wide aggregate statistics for the Admin Dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDTO {

    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long adminUsers;
    private long analystUsers;
    private long lockedUsers;
    private long newUsersThisMonth;

    private long totalExpenses;
    private long totalIncomes;
    private long totalCategories;
    private long totalBudgets;
    private long totalSavingsGoals;

    private BigDecimal totalExpenseAmount;
    private BigDecimal totalIncomeAmount;
    private BigDecimal totalSavingsAmount;
    private BigDecimal totalBudgetAmount;
}
