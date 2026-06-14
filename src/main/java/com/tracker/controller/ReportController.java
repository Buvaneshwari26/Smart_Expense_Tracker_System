package com.tracker.controller;

import com.tracker.dto.MonthlyTrendDTO;
import com.tracker.dto.SpendingReportDTO;
import com.tracker.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/category-spending")
    public ResponseEntity<List<SpendingReportDTO>> getSpendingReport(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SpendingReportDTO> response = reportService.getSpendingReport(userId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyTrendDTO>> getMonthlyTrend(
            @RequestParam Long userId,
            @RequestParam int year) {
        List<MonthlyTrendDTO> response = reportService.getMonthlyTrend(userId, year);
        return ResponseEntity.ok(response);
    }
}
