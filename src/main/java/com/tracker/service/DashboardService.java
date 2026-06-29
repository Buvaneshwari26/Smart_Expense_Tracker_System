package com.tracker.service;

import com.tracker.dto.*;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(Long userId) {
        userService.getUserEntity(userId);
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // Total income & expense (all-time)
        List<Income> allIncomes = incomeRepository.findByUserId(userId);
        List<Expense> allExpenses = expenseRepository.findByUserId(userId);

        BigDecimal totalIncome = allIncomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = allExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        // Monthly KPIs
        BigDecimal monthlyIncome = Objects.requireNonNullElse(
                incomeRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal monthlyExpense = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpense);
        BigDecimal savingsRate = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? monthlySavings.multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Top spending category this month
        List<Object[]> categoryTotals = expenseRepository.findCategoryWiseTotals(userId, month, year);
        String topCategory = categoryTotals.isEmpty() ? "N/A" : (String) categoryTotals.get(0)[0];

        // Budget progress (current month)
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        List<BudgetProgressDTO> budgetProgressList = budgets.stream().map(budget -> {
            BigDecimal spent = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndCategoryIdAndMonthAndYear(userId, budget.getCategory().getId(), month, year), BigDecimal.ZERO);
            BigDecimal remaining = budget.getBudgetAmount().subtract(spent);
            BigDecimal utilization = budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100)).divide(budget.getBudgetAmount(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return BudgetProgressDTO.builder()
                    .budgetId(budget.getId())
                    .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : "Deleted Category")
                    .limitAmount(budget.getBudgetAmount())
                    .spentAmount(spent)
                    .remainingAmount(remaining.max(BigDecimal.ZERO))
                    .utilizationPercent(utilization)
                    .isExceeded(spent.compareTo(budget.getBudgetAmount()) > 0)
                    .build();
        }).collect(Collectors.toList());

        // Recent transactions (top 10 combined)
        List<RecentTransactionDTO> recent = new ArrayList<>();
        allIncomes.stream().map(inc -> RecentTransactionDTO.builder()
                .id(inc.getId()).type("INCOME").amount(inc.getAmount())
                .date(inc.getDate()).categoryName(inc.getCategory() != null ? inc.getCategory().getName() : "Deleted Category")
                .description(inc.getDescription()).build()).forEach(recent::add);
        allExpenses.stream().map(exp -> RecentTransactionDTO.builder()
                .id(exp.getId()).type("EXPENSE").amount(exp.getAmount())
                .date(exp.getDate()).categoryName(exp.getCategory() != null ? exp.getCategory().getName() : "Deleted Category")
                .description(exp.getDescription()).build()).forEach(recent::add);
        recent = recent.stream()
                .sorted(Comparator.comparing(RecentTransactionDTO::getDate).reversed())
                .limit(10).collect(Collectors.toList());

        // Savings goals
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(userId);
        List<SavingsGoalProgressDTO> goalProgressList = goals.stream().map(goal -> {
            BigDecimal pct = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                            .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return SavingsGoalProgressDTO.builder()
                    .goalId(goal.getId()).goalName(goal.getGoalName())
                    .targetAmount(goal.getTargetAmount()).currentAmount(goal.getCurrentAmount())
                    .percentage(pct).targetDate(goal.getTargetDate()).build();
        }).collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .totalIncome(totalIncome).totalExpense(totalExpense).currentBalance(balance)
                .monthlyIncome(monthlyIncome).monthlyExpense(monthlyExpense)
                .monthlySavingsRate(savingsRate).topSpendingCategory(topCategory)
                .budgets(budgetProgressList).recentTransactions(recent).savingsGoals(goalProgressList)
                .build();
    }
}
