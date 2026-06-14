package com.tracker.service;

import com.tracker.dto.BudgetProgressDTO;
import com.tracker.dto.DashboardSummaryDTO;
import com.tracker.dto.RecentTransactionDTO;
import com.tracker.dto.SavingsGoalProgressDTO;
import com.tracker.model.Budget;
import com.tracker.model.Expense;
import com.tracker.model.Income;
import com.tracker.model.SavingsGoal;
import com.tracker.repository.BudgetRepository;
import com.tracker.repository.ExpenseRepository;
import com.tracker.repository.IncomeRepository;
import com.tracker.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserService userService;

    public DashboardService(IncomeRepository incomeRepository, ExpenseRepository expenseRepository,
                            BudgetRepository budgetRepository, SavingsGoalRepository savingsGoalRepository,
                            UserService userService) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(Long userId) {
        userService.getUserEntity(userId);

        // 1. Calculate Total Income
        List<Income> incomes = incomeRepository.findByUserId(userId);
        BigDecimal totalIncome = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Calculate Total Expense
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        BigDecimal totalExpense = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Compile Budget Status (Active Budgets Today)
        LocalDate today = LocalDate.now();
        List<Budget> activeBudgets = budgetRepository.findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(userId, today, today);
        List<BudgetProgressDTO> budgetProgressList = activeBudgets.stream().map(budget -> {
            // Calculate spent amount for category during budget's timeframe
            List<Expense> categoryExpenses = expenseRepository.findByUserIdAndCategoryIdAndDateBetween(
                    userId, budget.getCategory().getId(), budget.getStartDate(), budget.getEndDate()
            );
            BigDecimal spent = categoryExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal limitAmount = budget.getAmount();
            BigDecimal remaining = limitAmount.subtract(spent);
            boolean isExceeded = spent.compareTo(limitAmount) > 0;

            return BudgetProgressDTO.builder()
                    .budgetId(budget.getId())
                    .categoryName(budget.getCategory().getName())
                    .limitAmount(limitAmount)
                    .spentAmount(spent)
                    .remainingAmount(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining)
                    .isExceeded(isExceeded)
                    .build();
        }).collect(Collectors.toList());

        // 4. Compile Recent Transactions (Top 5 Combined Income/Expense sorted by date descending)
        List<RecentTransactionDTO> recentTransactions = new ArrayList<>();
        
        incomes.stream()
                .map(inc -> RecentTransactionDTO.builder()
                        .id(inc.getId())
                        .type("INCOME")
                        .amount(inc.getAmount())
                        .date(inc.getDate())
                        .categoryName(inc.getCategory().getName())
                        .description(inc.getDescription())
                        .build())
                .forEach(recentTransactions::add);

        expenses.stream()
                .map(exp -> RecentTransactionDTO.builder()
                        .id(exp.getId())
                        .type("EXPENSE")
                        .amount(exp.getAmount())
                        .date(exp.getDate())
                        .categoryName(exp.getCategory().getName())
                        .description(exp.getDescription())
                        .build())
                .forEach(recentTransactions::add);

        // Sort descending by date, then by ID, and limit to 5
        recentTransactions = recentTransactions.stream()
                .sorted(Comparator.comparing(RecentTransactionDTO::getDate).reversed()
                        .thenComparing(RecentTransactionDTO::getId).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // 5. Compile Savings Goals Progress
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(userId);
        List<SavingsGoalProgressDTO> goalProgressList = goals.stream().map(goal -> {
            BigDecimal percentage = BigDecimal.ZERO;
            if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                percentage = goal.getCurrentAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
            }

            return SavingsGoalProgressDTO.builder()
                    .goalId(goal.getId())
                    .title(goal.getTitle())
                    .targetAmount(goal.getTargetAmount())
                    .currentAmount(goal.getCurrentAmount())
                    .percentage(percentage)
                    .targetDate(goal.getTargetDate())
                    .build();
        }).collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .budgets(budgetProgressList)
                .recentTransactions(recentTransactions)
                .savingsGoals(goalProgressList)
                .build();
    }
}
