package com.tracker.service;

import com.tracker.dto.BudgetDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
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
        // Validate required fields
        if (budgetDTO.getCategoryId() == null) {
            throw new BadRequestException("Category is required to create a budget.");
        }

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

        // Validate month range
        if (m < 1 || m > 12) {
            throw new BadRequestException("Month must be between 1 and 12.");
        }
        if (y < 2000 || y > 2100) {
            throw new BadRequestException("Year must be a valid 4-digit year.");
        }

        BigDecimal amount = budgetDTO.getBudgetAmount();
        if (amount == null) amount = budgetDTO.getLimitAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget amount must be greater than zero.");
        }

        // Check for duplicate budget (same user, category, month, year)
        List<Budget> existing = budgetRepository.findByUserIdAndMonthAndYear(userId, m, y);
        final int finalM = m;
        final int finalY = y;
        Optional<Budget> duplicate = existing.stream()
                .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(category.getId()))
                .findFirst();
        if (duplicate.isPresent()) {
            throw new BadRequestException(
                    "A budget for category '" + category.getName() + "' already exists for "
                    + java.time.Month.of(finalM).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    + " " + finalY + ". Please edit the existing budget instead.");
        }

        try {
            Budget budget = Budget.builder()
                    .budgetAmount(amount)
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
        } catch (DataIntegrityViolationException e) {
            log.error("Budget creation constraint violation: {}", e.getMessage());
            throw new BadRequestException(
                    "A budget for this category and month already exists. Please edit the existing budget.");
        }
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

        if (budgetDTO.getCategoryId() == null) {
            throw new BadRequestException("Category is required.");
        }

        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);

        BigDecimal newAmount = budgetDTO.getBudgetAmount();
        if (newAmount == null) newAmount = budgetDTO.getLimitAmount();
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget amount must be greater than zero.");
        }

        budget.setBudgetAmount(newAmount);

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

        try {
            Budget saved = budgetRepository.save(budget);
            activityLogService.logActivity(budget.getUser(), "BUDGET_UPDATE",
                    "Updated budget of ID: " + budgetId + " (amount: ₹" + saved.getBudgetAmount() + ")");
            return mapToDTO(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("Budget update constraint violation: {}", e.getMessage());
            throw new BadRequestException("A budget for this category and month already exists.");
        }
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
                budget.getCategory() != null ? budget.getCategory().getId() : -1L,
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
                .categoryId(budget.getCategory() != null ? budget.getCategory().getId() : null)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : "Deleted Category")
                .budgetAmount(budgetAmt)
                .limitAmount(budgetAmt)
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
