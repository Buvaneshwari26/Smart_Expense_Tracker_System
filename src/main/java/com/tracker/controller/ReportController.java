package com.tracker.controller;

import com.tracker.security.SecurityUtils;
import com.tracker.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Financial reports and analytics")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly income/expense report")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return ResponseEntity.ok(reportService.getMonthlyReport(userId, m, y));
    }

    @GetMapping("/yearly")
    @Operation(summary = "Get yearly income/expense report")
    public ResponseEntity<Map<String, Object>> getYearlyReport(
            @RequestParam(required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(reportService.getYearlyReport(userId, y));
    }

    @GetMapping("/category")
    @Operation(summary = "Get category-wise expense breakdown")
    public ResponseEntity<Map<String, Object>> getCategoryReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return ResponseEntity.ok(reportService.getCategoryReport(userId, m, y));
    }

    @GetMapping("/income-vs-expense")
    @Operation(summary = "Get income vs expense comparison for the year")
    public ResponseEntity<Map<String, Object>> getIncomeVsExpenseReport(
            @RequestParam(required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(reportService.getIncomeVsExpenseReport(userId, y));
    }
}
