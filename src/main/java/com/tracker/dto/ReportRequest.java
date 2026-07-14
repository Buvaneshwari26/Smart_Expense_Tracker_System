package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private String type; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, USER_WISE, CATEGORY_WISE, BUDGET_WISE, SAVINGS_WISE, FINANCIAL_SUMMARY
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    
    private Integer month;
    private Integer year;
    private Integer quarter; // 1, 2, 3, 4
    private Long categoryId;
    private Long userId; // Admin can specify other user IDs; User/Analyst are restricted
}
