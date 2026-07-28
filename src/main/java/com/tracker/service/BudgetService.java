package com.tracker.service;

import com.tracker.dto.BudgetDTO;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ActivityLogService activityLogService;

    @Transactional
    public BudgetDTO createBudget(Long userId, BudgetDTO budgetDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);

        Integer m = budgetDTO.getMonth();
        Integer y = budgetDTO.getYear();
        if (m == null || y == null) {
            if (budgetDTO.getStartDate() != null) {
                m = budgetDTO.getStartDate().getMonthValue();
                y = budgetDTO.getStartDate().getYear();
            } else {
                m = LocalDate.now().getMonthValue();
                y = LocalDate.now().getYear();
            }
        }

        Budget budget = Budget.builder()
                .budgetAmount(budgetDTO.getBudgetAmount())
                .month(m)
                .year(y)
                .category(category)
                .user(user)
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("Budget created: {} for user {}", saved.getId(), userId);

        activityLogService.logActivity(user, "BUDGET_CREATE",
                "Created monthly budget for " + category.getName() + " of ₹" + saved.getBudgetAmount());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetDTO> getBudgetsByUserId(Long userId) {
        userService.getUserEntity(userId);
        return budgetRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetDTO getBudgetById(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));
        return mapToDTO(budget);
    }

    @Transactional
    public BudgetDTO updateBudget(Long userId, Long budgetId, BudgetDTO budgetDTO) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);
        budget.setBudgetAmount(budgetDTO.getBudgetAmount());

        Integer m = budgetDTO.getMonth();
        Integer y = budgetDTO.getYear();
        if (m == null || y == null) {
            if (budgetDTO.getStartDate() != null) {
                m = budgetDTO.getStartDate().getMonthValue();
                y = budgetDTO.getStartDate().getYear();
            } else {
                m = LocalDate.now().getMonthValue();
                y = LocalDate.now().getYear();
            }
        }

        budget.setMonth(m);
        budget.setYear(y);
        budget.setCategory(category);

        Budget saved = budgetRepository.save(budget);

        activityLogService.logActivity(budget.getUser(), "BUDGET_UPDATE",
                "Updated budget of ID: " + budgetId + " (amount: ₹" + saved.getBudgetAmount() + ")");

        return mapToDTO(saved);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));
        budgetRepository.delete(budget);
        log.info("Budget soft-deleted: {} for user {}", budgetId, userId);

        activityLogService.logActivity(budget.getUser(), "BUDGET_DELETE",
                "Deleted budget of ID: " + budgetId);
    }

    // ── DTO mapping with spend calculation ────────────────────────────────────

    private BudgetDTO mapToDTO(Budget budget) {
        LocalDate start = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate end   = start.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal spent = expenseRepository.sumByUserIdAndCategoryIdAndMonthAndYear(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                budget.getMonth(),
                budget.getYear());

        if (spent == null) spent = BigDecimal.ZERO;

        BigDecimal budgetAmt  = budget.getBudgetAmount() != null ? budget.getBudgetAmount() : BigDecimal.ZERO;
        BigDecimal remaining  = budgetAmt.subtract(spent);
        boolean exceeded      = spent.compareTo(budgetAmt) > 0;

        BigDecimal utilization = BigDecimal.ZERO;
        if (budgetAmt.compareTo(BigDecimal.ZERO) > 0) {
            utilization = spent.multiply(BigDecimal.valueOf(100))
                               .divide(budgetAmt, 2, RoundingMode.HALF_UP);
        }

        return BudgetDTO.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .budgetAmount(budgetAmt)
                .month(budget.getMonth())
                .year(budget.getYear())
                .startDate(start)
                .endDate(end)
                .spentAmount(spent)
                .remainingAmount(remaining)
                .utilizationPercent(utilization)
                .isExceeded(exceeded)
                .build();
    }
}
