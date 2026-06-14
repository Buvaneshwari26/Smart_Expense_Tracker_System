package com.tracker.service;

import com.tracker.dto.MonthlyTrendDTO;
import com.tracker.dto.SpendingReportDTO;
import com.tracker.model.Expense;
import com.tracker.model.Income;
import com.tracker.repository.ExpenseRepository;
import com.tracker.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserService userService;

    public ReportService(ExpenseRepository expenseRepository, IncomeRepository incomeRepository, UserService userService) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<SpendingReportDTO> getSpendingReport(Long userId, LocalDate startDate, LocalDate endDate) {
        userService.getUserEntity(userId);

        // Fetch expenses in date range
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        if (expenses.isEmpty()) {
            return new ArrayList<>();
        }

        // Sum total spent in the range
        BigDecimal totalSpentOverall = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by category name and sum amounts
        Map<String, BigDecimal> spendingByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        exp -> exp.getCategory().getName(),
                        Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // Create SpendingReportDTOs
        return spendingByCategory.entrySet().stream().map(entry -> {
            BigDecimal categorySpent = entry.getValue();
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalSpentOverall.compareTo(BigDecimal.ZERO) > 0) {
                percentage = categorySpent.multiply(new BigDecimal("100"))
                        .divide(totalSpentOverall, 2, RoundingMode.HALF_UP);
            }

            return SpendingReportDTO.builder()
                    .categoryName(entry.getKey())
                    .totalSpent(categorySpent)
                    .percentageOfTotal(percentage)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendDTO> getMonthlyTrend(Long userId, int year) {
        userService.getUserEntity(userId);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        // Fetch incomes and expenses in the year
        List<Income> incomes = incomeRepository.findByUserIdAndDateBetween(userId, startOfYear, endOfYear);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startOfYear, endOfYear);

        // Group by month
        Map<Month, BigDecimal> monthlyIncome = incomes.stream()
                .collect(Collectors.groupingBy(
                        inc -> inc.getDate().getMonth(),
                        Collectors.mapping(Income::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        Map<Month, BigDecimal> monthlyExpense = expenses.stream()
                .collect(Collectors.groupingBy(
                        exp -> exp.getDate().getMonth(),
                        Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        List<MonthlyTrendDTO> trends = new ArrayList<>();

        // Generate entry for each of the 12 months
        for (int i = 1; i <= 12; i++) {
            Month month = Month.of(i);
            String monthName = month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            BigDecimal totalInc = monthlyIncome.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal totalExp = monthlyExpense.getOrDefault(month, BigDecimal.ZERO);

            trends.add(MonthlyTrendDTO.builder()
                    .monthValue(i)
                    .monthName(monthName)
                    .totalIncome(totalInc)
                    .totalExpense(totalExp)
                    .build());
        }

        return trends;
    }
}
