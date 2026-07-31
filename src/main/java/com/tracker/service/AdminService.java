package com.tracker.service;

import com.tracker.dto.ActivityLogDTO;
import com.tracker.dto.AdminStatsDTO;
import com.tracker.model.ActivityLog;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service layer for Admin-specific operations:
 *   - System-wide statistics (GET /api/admin/stats)
 *   - Paginated activity logs across all users (GET /api/admin/activity-logs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository       userRepository;
    private final ExpenseRepository    expenseRepository;
    private final IncomeRepository     incomeRepository;
    private final CategoryRepository   categoryRepository;
    private final BudgetRepository     budgetRepository;
    private final ActivityLogRepository activityLogRepository;

    // ── Stats ────────────────────────────────────────────────────────────────

    /**
     * Aggregate system-wide statistics for the Admin Dashboard.
     * All repository queries reuse existing methods or JpaRepository.count().
     */
    @Transactional(readOnly = true)
    public AdminStatsDTO getAdminStats() {
        long totalUsers      = userRepository.count();
        long activeUsers     = userRepository.countByAccountActiveTrue();
        long totalExpenses   = expenseRepository.count();
        long totalIncomes    = incomeRepository.count();
        long totalCategories = categoryRepository.count();
        long totalBudgets    = budgetRepository.count();

        // sumSystemWideExpense / sumSystemWideIncome return null when table is empty
        BigDecimal totalExpenseAmount = expenseRepository.sumSystemWideExpense();
        if (totalExpenseAmount == null) totalExpenseAmount = BigDecimal.ZERO;

        BigDecimal totalIncomeAmount = incomeRepository.sumSystemWideIncome();
        if (totalIncomeAmount == null) totalIncomeAmount = BigDecimal.ZERO;

        log.debug("Admin stats: users={}, active={}, expenses={}, incomes={}, categories={}, budgets={}",
                totalUsers, activeUsers, totalExpenses, totalIncomes, totalCategories, totalBudgets);

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalExpenses(totalExpenses)
                .totalIncomes(totalIncomes)
                .totalCategories(totalCategories)
                .totalBudgets(totalBudgets)
                .totalExpenseAmount(totalExpenseAmount)
                .totalIncomeAmount(totalIncomeAmount)
                .build();
    }

    // ── Activity Logs ────────────────────────────────────────────────────────

    /**
     * Return a paginated view of ALL activity logs across every user,
     * sorted newest-first.  Admin-only endpoint.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getAllActivityLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return activityLogRepository.findAll(pageable).map(this::toDTO);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private ActivityLogDTO toDTO(ActivityLog log) {
        // Prefer username display name; fall back to email
        String usernameDisplay = (log.getUser().getUsername() != null
                && !log.getUser().getUsername().isEmpty())
                ? log.getUser().getUsername()
                : log.getUser().getEmail();

        return ActivityLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .userEmail(log.getUser().getEmail())
                .username(usernameDisplay)          // frontend: log.username
                .action(log.getAction())
                .description(log.getDescription())
                .details(log.getDescription())      // frontend: log.details
                .ipAddress(log.getIpAddress())
                .deviceInfo(log.getDeviceInfo())
                .createdAt(log.getCreatedAt())
                .timestamp(log.getCreatedAt())      // frontend: log.timestamp
                .build();
    }
}
