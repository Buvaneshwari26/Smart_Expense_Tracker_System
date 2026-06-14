package com.tracker.service;

import com.tracker.dto.BudgetDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.Budget;
import com.tracker.model.Category;
import com.tracker.model.CategoryType;
import com.tracker.model.Expense;
import com.tracker.model.User;
import com.tracker.repository.BudgetRepository;
import com.tracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    public BudgetService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository,
                         UserService userService, CategoryService categoryService) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Transactional
    public BudgetDTO createBudget(Long userId, BudgetDTO budgetDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);

        // 1. Validate category is EXPENSE
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Budgets can only be set for EXPENSE categories. Category '" + category.getName() + "' is of type " + category.getType());
        }

        // 2. Validate dates
        if (budgetDTO.getStartDate().isAfter(budgetDTO.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        // 3. Check for overlapping budgets for the same category
        List<Budget> overlapping = budgetRepository.findOverlappingBudgets(userId, category.getId(), budgetDTO.getStartDate(), budgetDTO.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("A budget limit for category '" + category.getName() + "' already exists and overlaps with the proposed dates: " +
                    budgetDTO.getStartDate() + " to " + budgetDTO.getEndDate());
        }

        Budget budget = Budget.builder()
                .amount(budgetDTO.getAmount())
                .startDate(budgetDTO.getStartDate())
                .endDate(budgetDTO.getEndDate())
                .category(category)
                .user(user)
                .build();

        Budget savedBudget = budgetRepository.save(budget);
        return mapToDTO(savedBudget);
    }

    @Transactional(readOnly = true)
    public List<BudgetDTO> getBudgetsByUserId(Long userId) {
        userService.getUserEntity(userId);
        return budgetRepository.findByUserId(userId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public BudgetDTO updateBudget(Long userId, Long budgetId, BudgetDTO budgetDTO) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId + " for user: " + userId));

        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Budgets can only be set for EXPENSE categories.");
        }

        if (budgetDTO.getStartDate().isAfter(budgetDTO.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        // Check overlapping excluding current budget
        List<Budget> overlapping = budgetRepository.findOverlappingBudgets(userId, category.getId(), budgetDTO.getStartDate(), budgetDTO.getEndDate());
        boolean hasOverlap = overlapping.stream().anyMatch(b -> !b.getId().equals(budgetId));
        if (hasOverlap) {
            throw new BadRequestException("Updated dates overlap with another budget limit for category '" + category.getName() + "'.");
        }

        budget.setAmount(budgetDTO.getAmount());
        budget.setStartDate(budgetDTO.getStartDate());
        budget.setEndDate(budgetDTO.getEndDate());
        budget.setCategory(category);

        Budget updatedBudget = budgetRepository.save(budget);
        return mapToDTO(updatedBudget);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId + " for user: " + userId));
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public String checkBudgetStatus(Long userId, Long categoryId) {
        userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(categoryId, userId);

        LocalDate today = LocalDate.now();
        Optional<Budget> activeBudgetOpt = budgetRepository
                .findByUserIdAndCategoryIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(userId, categoryId, today, today);

        if (activeBudgetOpt.isEmpty()) {
            return "No active budget configured for category '" + category.getName() + "' today.";
        }

        Budget activeBudget = activeBudgetOpt.get();
        
        // Sum expenses in the budget date range
        List<Expense> expenses = expenseRepository.findByUserIdAndCategoryIdAndDateBetween(
                userId, categoryId, activeBudget.getStartDate(), activeBudget.getEndDate()
        );

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal limitAmount = activeBudget.getAmount();

        if (totalSpent.compareTo(limitAmount) > 0) {
            BigDecimal exceededBy = totalSpent.subtract(limitAmount);
            return "EXCEEDED: Spent " + totalSpent + " of " + limitAmount + " (Exceeded by " + exceededBy + ") for category '" + category.getName() + "'";
        } else {
            BigDecimal remaining = limitAmount.subtract(totalSpent);
            return "OK: Spent " + totalSpent + " of " + limitAmount + " (Remaining: " + remaining + ") for category '" + category.getName() + "'";
        }
    }

    private BudgetDTO mapToDTO(Budget budget) {
        return BudgetDTO.builder()
                .id(budget.getId())
                .amount(budget.getAmount())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .build();
    }
}
