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

    /** Total number of registered users (non-deleted). */
    private long totalUsers;

    /** Number of users whose accountActive flag is true. */
    private long activeUsers;

    /** Total count of expense records across all users. */
    private long totalExpenses;

    /** Total count of income records across all users. */
    private long totalIncomes;

    /** Total count of category records across all users. */
    private long totalCategories;

    /** Total count of budget records across all users. */
    private long totalBudgets;

    /** System-wide sum of all expense amounts (may be null when table is empty). */
    private BigDecimal totalExpenseAmount;

    /** System-wide sum of all income amounts (may be null when table is empty). */
    private BigDecimal totalIncomeAmount;
}
