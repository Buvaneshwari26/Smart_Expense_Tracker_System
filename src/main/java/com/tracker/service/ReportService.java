package com.tracker.service;

import com.tracker.dto.ReportRequest;
import com.tracker.dto.ReportResponse;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;

    /**
     * Dispatcher method to generate a unified ReportResponse based on ReportRequest.
     */
    @Transactional(readOnly = true)
    public ReportResponse generateReport(ReportRequest request, User currentUser) {
        String typeStr = request.getType() != null ? request.getType().toUpperCase().trim() : "FINANCIAL_SUMMARY";
        Long userId = request.getUserId() != null ? request.getUserId() : currentUser.getId();

        // Security check: Only Admin can view reports for other users
        if (!currentUser.getRole().equals("ADMIN") && !userId.equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view reports for other users.");
        }

        // Security check: Only Admin can run USER_WISE reports
        if (typeStr.equals("USER_WISE") && !currentUser.getRole().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only Administrators can view User-wise reports.");
        }

        switch (typeStr) {
            case "DAILY":
                return generateDailyReport(userId, request, currentUser.getUsername());
            case "WEEKLY":
                return generateWeeklyReport(userId, request, currentUser.getUsername());
            case "MONTHLY":
                return generateMonthlyReportResponse(userId, request, currentUser.getUsername());
            case "QUARTERLY":
                return generateQuarterlyReport(userId, request, currentUser.getUsername());
            case "YEARLY":
                return generateYearlyReportResponse(userId, request, currentUser.getUsername());
            case "USER_WISE":
                return generateUserWiseReport(currentUser.getUsername());
            case "CATEGORY_WISE":
                return generateCategoryWiseReportResponse(userId, request, currentUser.getUsername());
            case "BUDGET_WISE":
                return generateBudgetWiseReport(userId, request, currentUser.getUsername());
            case "SAVINGS_WISE":
                return generateSavingsWiseReport(userId, currentUser.getUsername());
            case "FINANCIAL_SUMMARY":
            default:
                return generateFinancialSummaryReport(userId, request, currentUser.getUsername());
        }
    }

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

    // ─────────────────────────────────────────────────────────────────────────
    // REPORT IMPLEMENTATIONS
    // ─────────────────────────────────────────────────────────────────────────

    private ReportResponse generateDailyReport(Long userId, ReportRequest req, String username) {
        LocalDate date = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        List<Income> incomes = incomeRepository.findByUserIdAndDateBetween(userId, date, date);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, date, date);

        BigDecimal totalIncome = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalIncome", totalIncome);
        kpis.put("totalExpense", totalExpense);
        kpis.put("netBalance", balance);

        List<String> headers = List.of("Time/ID", "Type", "Category", "Amount", "Description");
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Income i : incomes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Time/ID", "INC-" + i.getId());
            row.put("Type", "Income");
            row.put("Category", i.getCategory() != null ? i.getCategory().getName() : "General");
            row.put("Amount", i.getAmount());
            row.put("Description", i.getDescription() != null ? i.getDescription() : "Income");
            rows.add(row);
        }

        for (Expense e : expenses) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Time/ID", "EXP-" + e.getId());
            row.put("Type", "Expense");
            row.put("Category", e.getCategory() != null ? e.getCategory().getName() : "General");
            row.put("Amount", e.getAmount());
            row.put("Description", e.getDescription() != null ? e.getDescription() : "Expense");
            rows.add(row);
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", List.of("Income", "Expense"));
        chartData.put("datasets", List.of(Map.of("data", List.of(totalIncome, totalExpense))));

        List<String> insights = new ArrayList<>();
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            insights.add("Warning: Today you spent more than you earned by ₹" + balance.abs() + ".");
        } else if (totalIncome.compareTo(BigDecimal.ZERO) > 0 || totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            insights.add("Net positive day. Savings rate is " + (totalIncome.compareTo(BigDecimal.ZERO) == 0 ? 0 : balance.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 2, RoundingMode.HALF_UP)) + "%.");
        } else {
            insights.add("No financial transactions recorded for today.");
        }

        return ReportResponse.builder()
                .title("Daily Financial Report for " + date)
                .type("DAILY")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateWeeklyReport(Long userId, ReportRequest req, String username) {
        LocalDate endDate = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : endDate.minusDays(6);

        List<Income> incomes = incomeRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        BigDecimal totalIncome = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalIncome", totalIncome);
        kpis.put("totalExpense", totalExpense);
        kpis.put("netBalance", balance);

        List<String> headers = List.of("Date", "Type", "Category", "Amount", "Description");
        List<Map<String, Object>> rows = new ArrayList<>();

        incomes.forEach(i -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Date", i.getDate().toString());
            r.put("Type", "Income");
            r.put("Category", i.getCategory() != null ? i.getCategory().getName() : "General");
            r.put("Amount", i.getAmount());
            r.put("Description", i.getSource() != null ? i.getSource() : "Income");
            rows.add(r);
        });

        expenses.forEach(e -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Date", e.getDate().toString());
            r.put("Type", "Expense");
            r.put("Category", e.getCategory() != null ? e.getCategory().getName() : "General");
            r.put("Amount", e.getAmount());
            r.put("Description", e.getDescription() != null ? e.getDescription() : "Expense");
            rows.add(r);
        });

        // Group by Date for line chart
        Map<LocalDate, BigDecimal> dailySpent = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getDate,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        List<String> labels = new ArrayList<>();
        List<BigDecimal> dataVals = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            labels.add(d.toString());
            dataVals.add(dailySpent.getOrDefault(d, BigDecimal.ZERO));
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("label", "Daily Spending", "data", dataVals)));

        List<String> insights = new ArrayList<>();
        insights.add("Weekly summary: Total income ₹" + totalIncome + ", total spent ₹" + totalExpense + ".");
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            insights.add("Attention: Weekly net balance is negative. Consider trimming non-essential expenses next week.");
        }

        return ReportResponse.builder()
                .title("Weekly Report (" + startDate + " to " + endDate + ")")
                .type("WEEKLY")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateMonthlyReportResponse(Long userId, ReportRequest req, String username) {
        LocalDate now = LocalDate.now();
        int month = req.getMonth() != null ? req.getMonth() : now.getMonthValue();
        int year = req.getYear() != null ? req.getYear() : now.getYear();

        BigDecimal totalIncome = Objects.requireNonNullElse(
                incomeRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal totalExpense = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalIncome", totalIncome);
        kpis.put("totalExpense", totalExpense);
        kpis.put("netBalance", balance);

        List<Object[]> categoryBreakdown = expenseRepository.findCategoryWiseTotals(userId, month, year);
        List<String> headers = List.of("Category", "Spent Amount", "Percentage");
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (Object[] row : categoryBreakdown) {
            String cat = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            BigDecimal pct = totalExpense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    amt.multiply(BigDecimal.valueOf(100)).divide(totalExpense, 2, RoundingMode.HALF_UP);

            Map<String, Object> rowMap = new LinkedHashMap<>();
            rowMap.put("Category", cat);
            rowMap.put("Spent Amount", amt);
            rowMap.put("Percentage", pct + "%");
            rows.add(rowMap);

            labels.add(cat);
            values.add(amt);
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("data", values)));

        List<String> insights = new ArrayList<>();
        insights.add("Monthly Report for " + Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
        if (!categoryBreakdown.isEmpty()) {
            insights.add("Top spending category: " + categoryBreakdown.get(0)[0] + " (₹" + categoryBreakdown.get(0)[1] + ").");
        }

        return ReportResponse.builder()
                .title("Monthly Financial Report — " + Month.of(month).name() + " " + year)
                .type("MONTHLY")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateQuarterlyReport(Long userId, ReportRequest req, String username) {
        int quarter = req.getQuarter() != null ? req.getQuarter() : ((LocalDate.now().getMonthValue() - 1) / 3) + 1;
        int year = req.getYear() != null ? req.getYear() : LocalDate.now().getYear();

        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;

        BigDecimal qIncome = BigDecimal.ZERO;
        BigDecimal qExpense = BigDecimal.ZERO;

        List<String> headers = List.of("Month", "Income", "Expense", "Net Balance");
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> incVals = new ArrayList<>();
        List<BigDecimal> expVals = new ArrayList<>();

        for (int m = startMonth; m <= endMonth; m++) {
            BigDecimal inc = Objects.requireNonNullElse(
                    incomeRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);
            BigDecimal exp = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);

            qIncome = qIncome.add(inc);
            qExpense = qExpense.add(exp);

            String mName = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Month", mName);
            r.put("Income", inc);
            r.put("Expense", exp);
            r.put("Net Balance", inc.subtract(exp));
            rows.add(r);

            labels.add(mName);
            incVals.add(inc);
            expVals.add(exp);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalIncome", qIncome);
        kpis.put("totalExpense", qExpense);
        kpis.put("netBalance", qIncome.subtract(qExpense));

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(
                Map.of("label", "Income", "data", incVals),
                Map.of("label", "Expense", "data", expVals)
        ));

        List<String> insights = List.of(
                "Quarterly summary for Q" + quarter + " " + year + ".",
                "Quarterly Income: ₹" + qIncome + " | Quarterly Expense: ₹" + qExpense
        );

        return ReportResponse.builder()
                .title("Quarterly Report — Q" + quarter + " " + year)
                .type("QUARTERLY")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateYearlyReportResponse(Long userId, ReportRequest req, String username) {
        int year = req.getYear() != null ? req.getYear() : LocalDate.now().getYear();

        BigDecimal yIncome = BigDecimal.ZERO;
        BigDecimal yExpense = BigDecimal.ZERO;

        List<String> headers = List.of("Month", "Income", "Expense", "Net Balance");
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> incVals = new ArrayList<>();
        List<BigDecimal> expVals = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            BigDecimal inc = Objects.requireNonNullElse(
                    incomeRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);
            BigDecimal exp = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndMonthAndYear(userId, m, year), BigDecimal.ZERO);

            yIncome = yIncome.add(inc);
            yExpense = yExpense.add(exp);

            String mName = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Month", mName);
            r.put("Income", inc);
            r.put("Expense", exp);
            r.put("Net Balance", inc.subtract(exp));
            rows.add(r);

            labels.add(mName);
            incVals.add(inc);
            expVals.add(exp);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalIncome", yIncome);
        kpis.put("totalExpense", yExpense);
        kpis.put("netBalance", yIncome.subtract(yExpense));
        kpis.put("monthlyAvgExpense", yExpense.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(
                Map.of("label", "Income", "data", incVals),
                Map.of("label", "Expense", "data", expVals)
        ));

        List<String> insights = List.of(
                "Yearly summary for " + year,
                "Yearly Income: ₹" + yIncome + " | Yearly Expense: ₹" + yExpense + " | Net savings: ₹" + yIncome.subtract(yExpense)
        );

        return ReportResponse.builder()
                .title("Yearly Report — " + year)
                .type("YEARLY")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateUserWiseReport(String username) {
        List<User> users = userRepository.findAll();

        List<String> headers = List.of("User ID", "Name", "Email", "Role", "Total Income", "Total Expense", "Net Balance", "Status");
        List<Map<String, Object>> rows = new ArrayList<>();

        BigDecimal systemTotalIncome = BigDecimal.ZERO;
        BigDecimal systemTotalExpense = BigDecimal.ZERO;

        for (User u : users) {
            BigDecimal inc = Objects.requireNonNullElse(incomeRepository.sumTotalByUserId(u.getId()), BigDecimal.ZERO);
            BigDecimal exp = Objects.requireNonNullElse(expenseRepository.sumTotalByUserId(u.getId()), BigDecimal.ZERO);

            systemTotalIncome = systemTotalIncome.add(inc);
            systemTotalExpense = systemTotalExpense.add(exp);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("User ID", u.getId());
            r.put("Name", u.getFullName());
            r.put("Email", u.getEmail());
            r.put("Role", u.getRole());
            r.put("Total Income", inc);
            r.put("Total Expense", exp);
            r.put("Net Balance", inc.subtract(exp));
            r.put("Status", u.getAccountActive() != null && u.getAccountActive() ? "Active" : "Inactive");
            rows.add(r);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalUsers", (long) users.size());
        kpis.put("activeUsers", users.stream().filter(u -> u.getAccountActive() != null && u.getAccountActive()).count());
        kpis.put("systemTotalIncome", systemTotalIncome);
        kpis.put("systemTotalExpense", systemTotalExpense);
        kpis.put("systemNetBalance", systemTotalIncome.subtract(systemTotalExpense));

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", List.of("Active Users", "Inactive Users"));
        chartData.put("datasets", List.of(Map.of("data", List.of(
                users.stream().filter(u -> u.getAccountActive() != null && u.getAccountActive()).count(),
                users.stream().filter(u -> u.getAccountActive() == null || !u.getAccountActive()).count()
        ))));

        List<String> insights = List.of(
                "System health check: " + users.size() + " registered accounts.",
                "Total system velocity is ₹" + systemTotalIncome.add(systemTotalExpense) + " in transactions."
        );

        return ReportResponse.builder()
                .title("System User-wise Summary Report")
                .type("USER_WISE")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateCategoryWiseReportResponse(Long userId, ReportRequest req, String username) {
        LocalDate now = LocalDate.now();
        int month = req.getMonth() != null ? req.getMonth() : now.getMonthValue();
        int year = req.getYear() != null ? req.getYear() : now.getYear();

        BigDecimal totalExpense = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);

        List<Object[]> categoryBreakdown = expenseRepository.findCategoryWiseTotals(userId, month, year);

        List<String> headers = List.of("Category", "Spent Amount", "Percentage");
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (Object[] row : categoryBreakdown) {
            String cat = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            BigDecimal pct = totalExpense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    amt.multiply(BigDecimal.valueOf(100)).divide(totalExpense, 2, RoundingMode.HALF_UP);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Category", cat);
            r.put("Spent Amount", amt);
            r.put("Percentage", pct + "%");
            rows.add(r);

            labels.add(cat);
            values.add(amt);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalExpense", totalExpense);
        kpis.put("categoryCount", (long) categoryBreakdown.size());

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("data", values)));

        List<String> insights = List.of(
                "Category-wise spending distribution for " + month + "/" + year,
                "Unique spending categories active: " + categoryBreakdown.size()
        );

        return ReportResponse.builder()
                .title("Category-wise Expense Report — " + Month.of(month).name() + " " + year)
                .type("CATEGORY_WISE")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateBudgetWiseReport(Long userId, ReportRequest req, String username) {
        LocalDate now = LocalDate.now();
        int month = req.getMonth() != null ? req.getMonth() : now.getMonthValue();
        int year = req.getYear() != null ? req.getYear() : now.getYear();

        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);

        List<String> headers = List.of("Category", "Budget Limit", "Spent Amount", "Remaining", "Spent %", "Status");
        List<Map<String, Object>> rows = new ArrayList<>();

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        long overspentCount = 0;

        List<String> labels = new ArrayList<>();
        List<BigDecimal> limits = new ArrayList<>();
        List<BigDecimal> spentVals = new ArrayList<>();

        for (Budget b : budgets) {
            BigDecimal limitVal = b.getBudgetAmount();
            // Fetch spent for this category in this month/year
            BigDecimal spent = Objects.requireNonNullElse(
                    expenseRepository.sumByUserIdAndCategoryIdAndMonthAndYear(userId, b.getCategory().getId(), month, year),
                    BigDecimal.ZERO
            );

            totalLimit = totalLimit.add(limitVal);
            totalSpent = totalSpent.add(spent);

            BigDecimal remaining = limitVal.subtract(spent);
            BigDecimal spentPct = limitVal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    spent.multiply(BigDecimal.valueOf(100)).divide(limitVal, 2, RoundingMode.HALF_UP);

            String status = remaining.compareTo(BigDecimal.ZERO) < 0 ? "Overspent" : "On Track";
            if (status.equals("Overspent")) {
                overspentCount++;
            }

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Category", b.getCategory().getName());
            r.put("Budget Limit", limitVal);
            r.put("Spent Amount", spent);
            r.put("Remaining", remaining);
            r.put("Spent %", spentPct + "%");
            r.put("Status", status);
            rows.add(r);

            labels.add(b.getCategory().getName());
            limits.add(limitVal);
            spentVals.add(spent);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalBudgetLimit", totalLimit);
        kpis.put("totalSpent", totalSpent);
        kpis.put("remainingBudget", totalLimit.subtract(totalSpent));
        kpis.put("budgetHealth", totalLimit.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalSpent.multiply(BigDecimal.valueOf(100)).divide(totalLimit, 2, RoundingMode.HALF_UP));
        kpis.put("overspentBudgetsCount", overspentCount);

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(
                Map.of("label", "Limit", "data", limits),
                Map.of("label", "Spent", "data", spentVals)
        ));

        List<String> insights = new ArrayList<>();
        insights.add("Budgets tracked: " + budgets.size());
        if (overspentCount > 0) {
            insights.add("Action Required: You have exceeded limits in " + overspentCount + " categories!");
        } else if (!budgets.isEmpty()) {
            insights.add("Great job! All category spending is within the allocated budget limit.");
        }

        return ReportResponse.builder()
                .title("Budget-wise Performance Report — " + Month.of(month).name() + " " + year)
                .type("BUDGET_WISE")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateSavingsWiseReport(Long userId, String username) {
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(userId);

        List<String> headers = List.of("Goal Name", "Target Amount", "Saved Amount", "Remaining", "Completion %", "Target Date", "Status");
        List<Map<String, Object>> rows = new ArrayList<>();

        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalSaved = BigDecimal.ZERO;

        List<String> labels = new ArrayList<>();
        List<BigDecimal> targets = new ArrayList<>();
        List<BigDecimal> savedVals = new ArrayList<>();

        for (SavingsGoal g : goals) {
            BigDecimal targetVal = g.getTargetAmount();
            BigDecimal savedVal = g.getCurrentAmount();

            totalTarget = totalTarget.add(targetVal);
            totalSaved = totalSaved.add(savedVal);

            BigDecimal remaining = targetVal.subtract(savedVal);
            BigDecimal completionPct = targetVal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    savedVal.multiply(BigDecimal.valueOf(100)).divide(targetVal, 2, RoundingMode.HALF_UP);

            String status = completionPct.compareTo(BigDecimal.valueOf(100)) >= 0 ? "Achieved" : "In Progress";

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Goal Name", g.getGoalName());
            r.put("Target Amount", targetVal);
            r.put("Saved Amount", savedVal);
            r.put("Remaining", remaining);
            r.put("Completion %", completionPct + "%");
            r.put("Target Date", g.getTargetDate() != null ? g.getTargetDate().toString() : "-");
            r.put("Status", status);
            rows.add(r);

            labels.add(g.getGoalName());
            targets.add(targetVal);
            savedVals.add(savedVal);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalSavingsTarget", totalTarget);
        kpis.put("totalSavingsSaved", totalSaved);
        kpis.put("savingsGap", totalTarget.subtract(totalSaved));
        kpis.put("completionRate", totalTarget.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalSaved.multiply(BigDecimal.valueOf(100)).divide(totalTarget, 2, RoundingMode.HALF_UP));

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(
                Map.of("label", "Target", "data", targets),
                Map.of("label", "Saved", "data", savedVals)
        ));

        List<String> insights = new ArrayList<>();
        insights.add("Active savings goals: " + goals.size() + ".");
        long achieved = goals.stream().filter(g -> g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0).count();
        if (achieved > 0) {
            insights.add("Awesome! You've fully achieved " + achieved + " savings goal(s)!");
        }

        return ReportResponse.builder()
                .title("Savings Goals Performance Report")
                .type("SAVINGS_WISE")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    private ReportResponse generateFinancialSummaryReport(Long userId, ReportRequest req, String username) {
        LocalDate now = LocalDate.now();
        int month = req.getMonth() != null ? req.getMonth() : now.getMonthValue();
        int year = req.getYear() != null ? req.getYear() : now.getYear();

        BigDecimal totalIncome = Objects.requireNonNullElse(
                incomeRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal totalExpense = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, month, year), BigDecimal.ZERO);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        // Fetch user budgets & budget totals
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        BigDecimal totalBudgetLimit = budgets.stream()
                .map(Budget::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fetch savings goals target and current saved
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(userId);
        BigDecimal totalSavingsTarget = goals.stream()
                .map(SavingsGoal::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSavingsSaved = goals.stream()
                .map(SavingsGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fetch highest single income and expense
        List<Income> monthlyIncomes = incomeRepository.findByUserIdAndDateBetween(
                userId, LocalDate.of(year, month, 1), LocalDate.of(year, month, YearMonth.of(year, month).lengthOfMonth())
        );
        List<Expense> monthlyExpenses = expenseRepository.findByUserIdAndDateBetween(
                userId, LocalDate.of(year, month, 1), LocalDate.of(year, month, YearMonth.of(year, month).lengthOfMonth())
        );

        BigDecimal highestIncome = monthlyIncomes.stream()
                .map(Income::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal highestExpense = monthlyExpenses.stream()
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Top spending categories
        List<Object[]> categoryBreakdown = expenseRepository.findCategoryWiseTotals(userId, month, year);
        String topCategoryName = "None";
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        if (!categoryBreakdown.isEmpty()) {
            topCategoryName = (String) categoryBreakdown.get(0)[0];
            topCategoryAmount = (BigDecimal) categoryBreakdown.get(0)[1];
        }

        // Compare with previous month
        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        BigDecimal prevIncome = Objects.requireNonNullElse(
                incomeRepository.sumByUserIdAndMonthAndYear(userId, prevMonth, prevYear), BigDecimal.ZERO);
        BigDecimal prevExpense = Objects.requireNonNullElse(
                expenseRepository.sumByUserIdAndMonthAndYear(userId, prevMonth, prevYear), BigDecimal.ZERO);

        BigDecimal incomeMoM = prevIncome.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalIncome.subtract(prevIncome).multiply(BigDecimal.valueOf(100)).divide(prevIncome, 2, RoundingMode.HALF_UP);
        BigDecimal expenseMoM = prevExpense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalExpense.subtract(prevExpense).multiply(BigDecimal.valueOf(100)).divide(prevExpense, 2, RoundingMode.HALF_UP);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalIncome", totalIncome);
        kpis.put("totalExpense", totalExpense);
        kpis.put("netBalance", balance);
        kpis.put("budgetUsed", totalBudgetLimit.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalExpense.multiply(BigDecimal.valueOf(100)).divide(totalBudgetLimit, 2, RoundingMode.HALF_UP));
        kpis.put("remainingBudget", totalBudgetLimit.subtract(totalExpense));
        kpis.put("savingsProgress", totalSavingsTarget.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                totalSavingsSaved.multiply(BigDecimal.valueOf(100)).divide(totalSavingsTarget, 2, RoundingMode.HALF_UP));
        kpis.put("highestIncome", highestIncome);
        kpis.put("highestExpense", highestExpense);
        kpis.put("topCategory", topCategoryName + " (₹" + topCategoryAmount + ")");
        kpis.put("incomeMoM", incomeMoM);
        kpis.put("expenseMoM", expenseMoM);

        List<String> headers = List.of("Metric", "Value", "Previous Month", "Growth Rate (MoM)");
        List<Map<String, Object>> rows = new ArrayList<>();

        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("Metric", "Monthly Income");
        row1.put("Value", totalIncome);
        row1.put("Previous Month", prevIncome);
        row1.put("Growth Rate (MoM)", incomeMoM + "%");
        rows.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("Metric", "Monthly Expense");
        row2.put("Value", totalExpense);
        row2.put("Previous Month", prevExpense);
        row2.put("Growth Rate (MoM)", expenseMoM + "%");
        rows.add(row2);

        Map<String, Object> row3 = new LinkedHashMap<>();
        row3.put("Metric", "Net Savings");
        row3.put("Value", balance);
        row3.put("Previous Month", prevIncome.subtract(prevExpense));
        row3.put("Growth Rate (MoM)", "-");
        rows.add(row3);

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", List.of("Income", "Expense", "Savings"));
        chartData.put("datasets", List.of(Map.of("data", List.of(totalIncome, totalExpense, balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO))));

        // Generate Financial Insights
        List<String> insights = new ArrayList<>();
        insights.add("Monthly savings rate is " + (totalIncome.compareTo(BigDecimal.ZERO) == 0 ? 0 : balance.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 2, RoundingMode.HALF_UP)) + "%.");

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            insights.add("Warning: You are running in a deficit this month. Your expenses exceed earnings by ₹" + balance.abs() + ".");
        } else {
            insights.add("Healthy financial standing: You saved ₹" + balance + " from earnings.");
        }

        if (totalBudgetLimit.compareTo(BigDecimal.ZERO) > 0 && totalExpense.compareTo(totalBudgetLimit) > 0) {
            insights.add("Critical: Total monthly expense has crossed the collective budget threshold of ₹" + totalBudgetLimit + ".");
        }

        if (!topCategoryName.equals("None")) {
            insights.add("Tip: Your highest spending category is " + topCategoryName + " (₹" + topCategoryAmount + "). Putting check limits on " + topCategoryName + " can save up to 10% more next month.");
        }

        return ReportResponse.builder()
                .title("Comprehensive Financial Summary — " + Month.of(month).name() + " " + year)
                .type("FINANCIAL_SUMMARY")
                .generatedAt(LocalDateTime.now())
                .generatedBy(username)
                .kpis(kpis)
                .headers(headers)
                .rows(rows)
                .chartData(chartData)
                .insights(insights)
                .build();
    }

    public byte[] exportReport(ReportRequest request, User currentUser, String format) {
        ReportResponse report = generateReport(request, currentUser);
        StringBuilder sb = new StringBuilder();
        
        sb.append("# ").append(report.getTitle() != null ? report.getTitle() : "Report").append("\n");
        sb.append("# Generated By: ").append(report.getGeneratedBy()).append(" at ").append(report.getGeneratedAt()).append("\n\n");
        
        if (report.getHeaders() != null && !report.getHeaders().isEmpty()) {
            sb.append(String.join(",", report.getHeaders())).append("\n");
        }
        if (report.getRows() != null && !report.getRows().isEmpty()) {
            for (Map<String, Object> row : report.getRows()) {
                List<String> values = new ArrayList<>();
                if (report.getHeaders() != null) {
                    for (String header : report.getHeaders()) {
                        Object val = row.get(header);
                        values.add(val != null ? "\"" + val.toString().replace("\"", "\"\"") + "\"" : "\"\"");
                    }
                }
                sb.append(String.join(",", values)).append("\n");
            }
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

