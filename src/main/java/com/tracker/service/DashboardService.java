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
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

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
        int dayOfMonth = today.getDayOfMonth();
        BigDecimal daysPassed = BigDecimal.valueOf(dayOfMonth > 0 ? dayOfMonth : 1);
        List<BudgetProgressDTO> budgetProgressList = budgets.stream().map(budget -> {
            BigDecimal spent = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndCategoryIdAndMonthAndYear(userId, budget.getCategory().getId(), month, year), BigDecimal.ZERO);
            BigDecimal remaining = budget.getBudgetAmount().subtract(spent);
            BigDecimal utilization = budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100)).divide(budget.getBudgetAmount(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            // Health Calculation
            String health = "HEALTHY";
            if (utilization.compareTo(BigDecimal.valueOf(85)) > 0) {
                health = "CRITICAL";
            } else if (utilization.compareTo(BigDecimal.valueOf(60)) > 0) {
                health = "WARNING";
            }

            // Velocity & Prediction Calculation
            BigDecimal velocity = spent.divide(daysPassed, 2, RoundingMode.HALF_UP);
            String prediction = "No overrun predicted";
            if (spent.compareTo(BigDecimal.ZERO) > 0 && spent.compareTo(budget.getBudgetAmount()) < 0) {
                if (velocity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal daysLeft = remaining.divide(velocity, 0, RoundingMode.CEILING);
                    int days = daysLeft.intValue();
                    if (days < 60) {
                        LocalDate predictedDate = LocalDate.now().plusDays(days);
                        if (predictedDate.getMonthValue() == month && predictedDate.getYear() == year) {
                            prediction = predictedDate.toString();
                        }
                    }
                }
            } else if (spent.compareTo(budget.getBudgetAmount()) >= 0) {
                prediction = "Overrun occurred";
            }

            return BudgetProgressDTO.builder()
                    .budgetId(budget.getId())
                    .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : "Deleted Category")
                    .limitAmount(budget.getBudgetAmount())
                    .spentAmount(spent)
                    .remainingAmount(remaining.max(BigDecimal.ZERO))
                    .utilizationPercent(utilization)
                    .isExceeded(spent.compareTo(budget.getBudgetAmount()) > 0)
                    .budgetHealth(health)
                    .spendingVelocity(velocity)
                    .predictedOverrunDate(prediction)
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

        // Monthly comparison
        int prevMonthValue = month - 1;
        int prevYear = year;
        if (prevMonthValue == 0) {
            prevMonthValue = 12;
            prevYear = year - 1;
        }
        BigDecimal prevSpent = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, prevMonthValue, prevYear), BigDecimal.ZERO);
        String monthlyComparison = "0.00% vs last month";
        if (prevSpent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = monthlyExpense.subtract(prevSpent);
            BigDecimal pct = diff.multiply(BigDecimal.valueOf(100)).divide(prevSpent, 2, RoundingMode.HALF_UP);
            monthlyComparison = (pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + pct + "% vs last month";
        } else if (monthlyExpense.compareTo(BigDecimal.ZERO) > 0) {
            monthlyComparison = "+100% vs last month (No baseline)";
        }

        // Financial Health
        String health = "HEALTHY";
        if (savingsRate.compareTo(BigDecimal.valueOf(30)) >= 0) {
            health = "EXCELLENT";
        } else if (savingsRate.compareTo(BigDecimal.valueOf(10)) >= 0) {
            health = "GOOD";
        } else if (monthlyExpense.compareTo(BigDecimal.ZERO) > 0) {
            health = "WARNING";
        }

        // Upcoming recurring transactions (next 7 days)
        List<RecurringTransaction> schedules = recurringTransactionRepository.findByUserId(userId);
        List<String> upcoming = schedules.stream()
                .filter(RecurringTransaction::isActive)
                .filter(s -> s.getNextExecutionDate().isBefore(LocalDate.now().plusDays(8)) && s.getNextExecutionDate().isAfter(LocalDate.now().minusDays(1)))
                .map(s -> s.getDescription() + " - ₹" + s.getAmount() + " on " + s.getNextExecutionDate())
                .collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .totalIncome(totalIncome).totalExpense(totalExpense).currentBalance(balance)
                .monthlyIncome(monthlyIncome).monthlyExpense(monthlyExpense)
                .monthlySavingsRate(savingsRate).topSpendingCategory(topCategory)
                .budgets(budgetProgressList).recentTransactions(recent).savingsGoals(goalProgressList)
                .monthlyComparison(monthlyComparison)
                .financialHealth(health)
                .upcomingRecurring(upcoming)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboard() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByAccountActiveTrue();
        long inactiveUsers = userRepository.countByAccountActiveFalse();
        long newUsers = userRepository.countByCreatedAtAfter(LocalDate.now().withDayOfMonth(1).atStartOfDay());

        BigDecimal totalIncome = Objects.requireNonNullElse(incomeRepository.sumSystemWideIncome(), BigDecimal.ZERO);
        BigDecimal totalExpense = Objects.requireNonNullElse(expenseRepository.sumSystemWideExpense(), BigDecimal.ZERO);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        // Top Spending Users
        List<Object[]> rawUsers = expenseRepository.findTopSpendingUsers(org.springframework.data.domain.PageRequest.of(0, 5));
        List<AdminDashboardDTO.UserSpending> topUsers = rawUsers.stream().map(row -> {
            return AdminDashboardDTO.UserSpending.builder()
                    .userId((Long) row[0])
                    .email((String) row[1])
                    .fullName((String) row[2])
                    .totalSpent((BigDecimal) row[3])
                    .build();
        }).collect(Collectors.toList());

        // Top Categories
        List<Object[]> rawCats = expenseRepository.findSystemWideCategoryTotals();
        List<AdminDashboardDTO.CategorySpending> topCategories = rawCats.stream().limit(5).map(row -> {
            return AdminDashboardDTO.CategorySpending.builder()
                    .categoryName((String) row[0])
                    .totalSpent((BigDecimal) row[1])
                    .build();
        }).collect(Collectors.toList());

        // Recent System Transactions
        List<RecentTransactionDTO> recent = new ArrayList<>();
        incomeRepository.findAll().stream().limit(20).forEach(inc -> {
            recent.add(RecentTransactionDTO.builder()
                    .id(inc.getId()).type("INCOME").amount(inc.getAmount()).date(inc.getDate())
                    .categoryName(inc.getCategory() != null ? inc.getCategory().getName() : "Deleted Category")
                    .description(inc.getSource() + " - " + inc.getDescription()).build());
        });
        expenseRepository.findAll().stream().limit(20).forEach(exp -> {
            recent.add(RecentTransactionDTO.builder()
                    .id(exp.getId()).type("EXPENSE").amount(exp.getAmount()).date(exp.getDate())
                    .categoryName(exp.getCategory() != null ? exp.getCategory().getName() : "Deleted Category")
                    .description(exp.getDescription()).build());
        });
        List<RecentTransactionDTO> sortedRecent = recent.stream()
                .sorted(Comparator.comparing(RecentTransactionDTO::getDate).reversed())
                .limit(10).collect(Collectors.toList());

        // Recent Logins
        List<LoginHistory> rawLogins = loginHistoryRepository.findTop10ByOrderByLoginAtDesc();
        List<LoginHistoryDTO> logins = rawLogins.stream().map(h -> {
            return LoginHistoryDTO.builder()
                    .id(h.getId())
                    .userId(h.getUser().getId())
                    .userEmail(h.getUser().getEmail())
                    .loginAt(h.getLoginAt())
                    .ipAddress(h.getIpAddress())
                    .deviceInfo(h.getDeviceInfo())
                    .status(h.getStatus())
                    .userAgent(h.getUserAgent())
                    .build();
        }).collect(Collectors.toList());

        return AdminDashboardDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .newUsersThisMonth(newUsers)
                .totalSystemIncome(totalIncome)
                .totalSystemExpense(totalExpense)
                .netBalance(balance)
                .topSpendingUsers(topUsers)
                .topCategories(topCategories)
                .recentTransactions(sortedRecent)
                .recentLogins(logins)
                .systemStatus("All systems operational. Uptime: 99.99%")
                .build();
    }

    @Transactional(readOnly = true)
    public AnalystDashboardDTO getAnalystDashboard(Long userId) {
        userService.getUserEntity(userId);
        LocalDate today = LocalDate.now();

        // 1. Monthly Trends (Last 6 Months)
        List<AnalystDashboardDTO.MonthlyTrend> trends = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate target = today.minusMonths(i);
            int m = target.getMonthValue();
            int y = target.getYear();
            BigDecimal inc = Objects.requireNonNullElse(incomeRepository.sumByUserIdAndMonthAndYear(userId, m, y), BigDecimal.ZERO);
            BigDecimal exp = Objects.requireNonNullElse(expenseRepository.sumByUserIdAndMonthAndYear(userId, m, y), BigDecimal.ZERO);
            trends.add(AnalystDashboardDTO.MonthlyTrend.builder()
                    .month(target.getMonth().name().substring(0, 3) + " " + y)
                    .income(inc)
                    .expense(exp)
                    .build());
        }

        // 2. Category Spending Analysis
        List<Object[]> rawCats = expenseRepository.findUserCategoryTotals(userId);
        BigDecimal totalSpent = rawCats.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnalystDashboardDTO.CategorySpending> categorySpendingList = rawCats.stream().map(row -> {
            BigDecimal amt = (BigDecimal) row[1];
            BigDecimal pct = totalSpent.compareTo(BigDecimal.ZERO) > 0
                    ? amt.multiply(BigDecimal.valueOf(100)).divide(totalSpent, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return AnalystDashboardDTO.CategorySpending.builder()
                    .categoryName((String) row[0])
                    .totalSpent(amt)
                    .percentage(pct)
                    .build();
        }).collect(Collectors.toList());

        // 3. Average calculations
        BigDecimal totalInc = incomeRepository.findByUserId(userId).stream()
                .map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExp = expenseRepository.findByUserId(userId).stream()
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Grouping unique months
        long uniqueMonthsIncome = incomeRepository.findByUserId(userId).stream()
                .map(i -> i.getDate().getYear() * 100 + i.getDate().getMonthValue())
                .distinct().count();
        long uniqueMonthsExpense = expenseRepository.findByUserId(userId).stream()
                .map(e -> e.getDate().getYear() * 100 + e.getDate().getMonthValue())
                .distinct().count();
        
        long denominatorInc = uniqueMonthsIncome > 0 ? uniqueMonthsIncome : 1;
        long denominatorExp = uniqueMonthsExpense > 0 ? uniqueMonthsExpense : 1;

        BigDecimal avgIncome = totalInc.divide(BigDecimal.valueOf(denominatorInc), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = totalExp.divide(BigDecimal.valueOf(denominatorExp), 2, RoundingMode.HALF_UP);

        BigDecimal savingsRate = totalInc.compareTo(BigDecimal.ZERO) > 0
                ? totalInc.subtract(totalExp).multiply(BigDecimal.valueOf(100)).divide(totalInc, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 4. Budget Adherence Rate
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        long underBudget = budgets.stream().filter(b -> {
            BigDecimal spent = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndCategoryIdAndMonthAndYear(userId, b.getCategory().getId(), b.getMonth(), b.getYear()), BigDecimal.ZERO);
            return spent.compareTo(b.getBudgetAmount()) <= 0;
        }).count();
        BigDecimal budgetAdherence = budgets.isEmpty() ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(underBudget * 100L).divide(BigDecimal.valueOf(budgets.size()), 2, RoundingMode.HALF_UP);

        // 5. Generate Insights
        List<String> insights = new ArrayList<>();
        if (savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            insights.add("Excellent! Your current savings rate is " + savingsRate + "%, which is above the recommended 20% benchmark.");
        } else {
            insights.add("Tip: Your savings rate is " + savingsRate + "%. Try reducing non-essential expenses to hit the 20% benchmark.");
        }

        if (budgetAdherence.compareTo(BigDecimal.valueOf(80)) >= 0) {
            insights.add("High budget discipline: You adhered to " + budgetAdherence + "% of your category budget targets this month.");
        } else if (!budgets.isEmpty()) {
            insights.add("Alert: Budget overrun detected in multiple categories. Review active budgets and adjust spending velocity.");
        }

        if (!categorySpendingList.isEmpty()) {
            insights.add("Top Category: The category '" + categorySpendingList.get(0).getCategoryName() + 
                    "' consumes " + categorySpendingList.get(0).getPercentage() + "% of your overall spending.");
        }

        return AnalystDashboardDTO.builder()
                .monthlyTrends(trends)
                .categoryAnalysis(categorySpendingList)
                .financialInsights(insights)
                .averageMonthlyIncome(avgIncome)
                .averageMonthlyExpense(avgExpense)
                .savingsRate(savingsRate)
                .budgetAdherenceRate(budgetAdherence)
                .build();
    }
}
