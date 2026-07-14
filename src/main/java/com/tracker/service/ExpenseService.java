package com.tracker.service;

import com.tracker.dto.ExpenseDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final BudgetRepository budgetRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ActivityLogService activityLogService;
    private final IncomeRepository incomeRepository;

    @Transactional
    public ExpenseDTO addExpense(Long userId, ExpenseDTO expenseDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(expenseDTO.getCategoryId(), userId);

        Expense expense = Expense.builder()
                .amount(expenseDTO.getAmount())
                .date(expenseDTO.getDate() != null ? expenseDTO.getDate() : LocalDate.now())
                .description(expenseDTO.getDescription())
                .category(category)
                .user(user)
                .build();

        Expense saved = expenseRepository.save(expense);
        log.info("Expense added: {} for user {}", saved.getId(), userId);

        // Record activity log
        activityLogService.logActivity(user, "EXPENSE_CREATE", "Added expense of ₹" + saved.getAmount() + " in Category: " + category.getName());

        // Check budget and send alert if exceeded
        checkBudgetAlert(user, category, saved.getDate());

        // Check large expense threshold (>20% of monthly income)
        checkLargeExpenseAlert(user, saved);

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseDTO> getExpensesByUserId(Long userId, Pageable pageable) {
        userService.getUserEntity(userId);
        return expenseRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long userId, Long expenseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAccess = auth != null && auth.getAuthorities().stream().anyMatch(a -> 
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ANALYST"));
        Expense expense = hasAccess
                ? expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId))
                : expenseRepository.findByIdAndUserId(expenseId, userId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        return mapToDTO(expense);
    }

    @Transactional
    public ExpenseDTO updateExpense(Long userId, Long expenseId, ExpenseDTO expenseDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Expense expense = isAdmin
                ? expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId))
                : expenseRepository.findByIdAndUserId(expenseId, userId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));

        Category category = categoryService.getCategoryEntity(expenseDTO.getCategoryId(), userId);
        expense.setAmount(expenseDTO.getAmount());
        expense.setDate(expenseDTO.getDate());
        expense.setDescription(expenseDTO.getDescription());
        expense.setCategory(category);

        Expense saved = expenseRepository.save(expense);
        
        // Record activity log
        activityLogService.logActivity(expense.getUser(), "EXPENSE_UPDATE", "Updated expense of ID: " + expenseId + " to ₹" + saved.getAmount());

        // Check budget limits again after update
        checkBudgetAlert(expense.getUser(), category, saved.getDate());

        return mapToDTO(saved);
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Expense expense = isAdmin
                ? expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId))
                : expenseRepository.findByIdAndUserId(expenseId, userId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        expenseRepository.delete(expense); // Triggers soft delete via @SQLDelete
        log.info("Expense soft-deleted: {} for user {}", expenseId, userId);
        
        // Record activity log
        activityLogService.logActivity(expense.getUser(), "EXPENSE_DELETE", "Deleted expense of ID: " + expenseId + " (amount: ₹" + expense.getAmount() + ")");
    }

    @Transactional(readOnly = true)
    public Page<ExpenseDTO> searchExpenses(Long userId, String keyword, Long categoryId,
                                            LocalDate startDate, LocalDate endDate,
                                            java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount, Pageable pageable) {
        userService.getUserEntity(userId);
        return expenseRepository.searchExpenses(userId, keyword, categoryId, startDate, endDate, minAmount, maxAmount, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDTO> getExpensesByCategory(Long userId, Long categoryId) {
        userService.getUserEntity(userId);
        return expenseRepository.findByUserIdAndCategoryId(userId, categoryId,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private void checkBudgetAlert(User user, Category category, LocalDate date) {
        try {
            int month = date.getMonthValue();
            int year = date.getYear();
            List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
            budgets.stream()
                    .filter(b -> b.getCategory().getId().equals(category.getId()))
                    .findFirst()
                    .ifPresent(budget -> {
                        java.math.BigDecimal spent = expenseRepository.sumByUserIdAndCategoryIdAndMonthAndYear(user.getId(), category.getId(), month, year);
                        if (spent != null) {
                            java.math.BigDecimal limit = budget.getBudgetAmount();
                            java.math.BigDecimal eightyPercent = limit.multiply(new java.math.BigDecimal("0.80"));
                            
                            if (spent.compareTo(limit) > 0) {
                                Notification notification = Notification.builder()
                                        .title("Budget Exceeded!")
                                        .message("Your " + category.getName() + " budget of ₹" + limit +
                                                 " has been exceeded. Total spent: ₹" + spent)
                                        .user(user)
                                        .isRead(false)
                                        .build();
                                notificationRepository.save(notification);
                                emailService.sendBudgetExceededAlert(user.getEmail(), user.getUsername(),
                                        category.getName(), limit, spent);
                            } else if (spent.compareTo(eightyPercent) >= 0) {
                                java.math.BigDecimal percent = spent.multiply(new java.math.BigDecimal("100"))
                                        .divide(limit, 2, java.math.RoundingMode.HALF_UP);
                                Notification notification = Notification.builder()
                                        .title("Budget Alert (80% Reached)")
                                        .message("Warning: You have reached " + percent + "% of your ₹" + limit + 
                                                 " budget for " + category.getName() + ". Total spent: ₹" + spent)
                                        .user(user)
                                        .isRead(false)
                                        .build();
                                notificationRepository.save(notification);
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("Error checking budget alert: {}", e.getMessage());
        }
    }

    private void checkLargeExpenseAlert(User user, Expense expense) {
        try {
            LocalDate date = expense.getDate();
            java.math.BigDecimal monthlyIncome = incomeRepository.sumByUserIdAndMonthAndYear(user.getId(), date.getMonthValue(), date.getYear());
            if (monthlyIncome != null && monthlyIncome.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal threshold = monthlyIncome.multiply(new java.math.BigDecimal("0.20"));
                if (expense.getAmount().compareTo(threshold) > 0) {
                    Notification notification = Notification.builder()
                            .title("Large Expense Alert")
                            .message("Warning: Expense of ₹" + expense.getAmount() + " exceeds 20% of your current monthly income of ₹" + monthlyIncome)
                            .user(user)
                            .isRead(false)
                            .build();
                    notificationRepository.save(notification);
                }
            }
        } catch (Exception e) {
            log.error("Error checking large expense alert: {}", e.getMessage());
        }
    }

    public ExpenseDTO mapToDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .description(expense.getDescription())
                .categoryId(expense.getCategory() != null ? expense.getCategory().getId() : null)
                .categoryName(expense.getCategory() != null ? expense.getCategory().getName() : "Deleted Category")
                .createdAt(expense.getCreatedAt())
                .build();
    }

    public List<Expense> getExpenseEntitiesByUserId(Long userId) {
        return expenseRepository.findByUserId(userId);
    }
}
