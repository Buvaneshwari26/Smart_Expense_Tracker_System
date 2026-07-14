package com.tracker.service;

import com.tracker.dto.ReportRequest;
import com.tracker.dto.ReportResponse;
import com.tracker.model.Budget;
import com.tracker.model.Category;
import com.tracker.model.CategoryType;
import com.tracker.model.User;
import com.tracker.repository.BudgetRepository;
import com.tracker.repository.ExpenseRepository;
import com.tracker.repository.IncomeRepository;
import com.tracker.repository.SavingsGoalRepository;
import com.tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService & ReportExportService Tests")
class ReportServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private ReportExportService reportExportService;
    private User testUser;

    @BeforeEach
    void setUp() {
        reportExportService = new ReportExportService();
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();
    }

    @Test
    @DisplayName("Should generate Financial Summary successfully")
    void shouldGenerateFinancialSummarySuccessfully() {
        ReportRequest request = new ReportRequest();
        request.setType("FINANCIAL_SUMMARY");
        request.setMonth(10);
        request.setYear(2026);
        request.setUserId(1L);

        when(incomeRepository.sumByUserIdAndMonthAndYear(1L, 10, 2026)).thenReturn(new BigDecimal("5000.00"));
        when(expenseRepository.sumByUserIdAndMonthAndYear(1L, 10, 2026)).thenReturn(new BigDecimal("3000.00"));

        List<Budget> budgets = new ArrayList<>();
        budgets.add(Budget.builder()
                .id(1L)
                .budgetAmount(new BigDecimal("1000.00"))
                .category(Category.builder().name("Food").type(CategoryType.EXPENSE).build())
                .build());
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 10, 2026)).thenReturn(budgets);

        ReportResponse response = reportService.generateReport(request, testUser);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("FINANCIAL_SUMMARY");
        assertThat(response.getKpis().get("totalIncome")).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.getKpis().get("totalExpense")).isEqualTo(new BigDecimal("3000.00"));
        assertThat(response.getKpis().get("netBalance")).isEqualTo(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("Should export reports to PDF, Excel, and CSV successfully")
    void shouldExportReportsSuccessfully() throws Exception {
        ReportResponse report = new ReportResponse();
        report.setType("FINANCIAL_SUMMARY");
        report.setTitle("Financial Summary Report");
        report.setGeneratedAt(java.time.LocalDateTime.now());
        report.setGeneratedBy("testuser");
        report.setHeaders(List.of("Category", "Amount"));
        report.setRows(List.of(
                java.util.Map.of("Category", "Food", "Amount", 500.0),
                java.util.Map.of("Category", "Rent", "Amount", 1200.0)
        ));
        report.setKpis(java.util.Map.of("totalIncome", 5000.0, "totalExpense", 1700.0));
        report.setInsights(List.of("Spending is within budget limits."));

        // Test PDF
        byte[] pdfBytes = reportExportService.exportToPdf(report);
        assertThat(pdfBytes).isNotEmpty();

        // Test Excel
        byte[] excelBytes = reportExportService.exportToExcel(report);
        assertThat(excelBytes).isNotEmpty();

        // Test CSV
        byte[] csvBytes = reportExportService.exportToCsv(report);
        assertThat(csvBytes).isNotEmpty();
    }
}
