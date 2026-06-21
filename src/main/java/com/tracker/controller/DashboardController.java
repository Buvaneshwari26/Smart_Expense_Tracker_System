package com.tracker.controller;

import com.tracker.dto.DashboardSummaryDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard KPIs, summaries, and analytics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get full dashboard summary with KPIs, budgets, goals, and recent transactions")
    public ResponseEntity<DashboardSummaryDTO> getDashboard() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getDashboardSummary(userId));
    }
}
