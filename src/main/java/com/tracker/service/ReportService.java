package com.tracker.service;

import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyReport(Long userId, int month, int year) {
        userService.getUserEntity(userId);
        BigDecimal totalIncome = Objects.requireNonNullElse(
                incomeRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal totalExpense = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<Object[]> categoryBreakdown = expenseRepository.findCategoryWiseTotals(userId, month, year);
        List<Map<String, Object>> categories = categoryBreakdown.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("category", row[0]);
            map.put("amount", row[1]);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("month", month);
        report.put("year", year);
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("balance", balance);
        report.put("categoryBreakdown", categories);
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getYearlyReport(Long userId, int year) {
        userService.getUserEntity(userId);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("year", year);

        List<Map<String, Object>> monthlyData = new ArrayList<>();
        BigDecimal yearlyIncome = BigDecimal.ZERO;
        BigDecimal yearlyExpense = BigDecimal.ZERO;

        for (int m = 1; m <= 12; m++) {
            BigDecimal income = Objects.requireNonNullElse(
                    incomeRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);
            BigDecimal expense = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);
            yearlyIncome = yearlyIncome.add(income);
            yearlyExpense = yearlyExpense.add(expense);

            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", m);
            monthData.put("income", income);
            monthData.put("expense", expense);
            monthData.put("balance", income.subtract(expense));
            monthlyData.add(monthData);
        }

        report.put("totalIncome", yearlyIncome);
        report.put("totalExpense", yearlyExpense);
        report.put("balance", yearlyIncome.subtract(yearlyExpense));
        report.put("monthlyData", monthlyData);
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCategoryReport(Long userId, int month, int year) {
        userService.getUserEntity(userId);
        List<Object[]> categories = expenseRepository.findCategoryWiseTotals(userId, month, year);

        List<Map<String, Object>> data = categories.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("category", row[0]);
            map.put("totalSpent", row[1]);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("month", month);
        report.put("year", year);
        report.put("categories", data);
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getIncomeVsExpenseReport(Long userId, int year) {
        userService.getUserEntity(userId);
        List<Map<String, Object>> comparison = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            BigDecimal income = Objects.requireNonNullElse(
                    incomeRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);
            BigDecimal expense = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);

            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", m);
            monthData.put("income", income);
            monthData.put("expense", expense);
            comparison.add(monthData);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("year", year);
        report.put("data", comparison);
        return report;
    }
}
