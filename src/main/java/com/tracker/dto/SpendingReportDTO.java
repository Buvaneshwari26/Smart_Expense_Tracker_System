package com.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingReportDTO {
    private String categoryName;
    private BigDecimal totalSpent;
    private BigDecimal percentageOfTotal; // e.g. 15.2%
}
