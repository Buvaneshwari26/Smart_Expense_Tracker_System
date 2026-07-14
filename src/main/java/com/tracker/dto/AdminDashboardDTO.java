package com.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDTO {
    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;
    private Long newUsersThisMonth;
    
    private BigDecimal totalSystemIncome;
    private BigDecimal totalSystemExpense;
    private BigDecimal netBalance;

    private List<UserSpending> topSpendingUsers;
    private List<CategorySpending> topCategories;
    
    private List<RecentTransactionDTO> recentTransactions;
    private List<LoginHistoryDTO> recentLogins;
    private String systemStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSpending {
        private Long userId;
        private String email;
        private String fullName;
        private BigDecimal totalSpent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySpending {
        private String categoryName;
        private BigDecimal totalSpent;
    }
}
