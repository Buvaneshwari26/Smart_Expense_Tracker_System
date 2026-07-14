package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String title;
    private String type;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private Map<String, Object> kpis; // standard summary metrics (e.g. totalIncome, totalExpense, balance, savingsProgress)
    private List<String> headers; // Table columns
    private List<Map<String, Object>> rows; // Table rows mapping column to value
    private Map<String, Object> chartData; // Labels, datasets for frontend charts
    private List<String> insights; // text-based analysis
}
