package com.tracker.controller;

import com.tracker.dto.ActivityLogDTO;
import com.tracker.dto.AdminStatsDTO;
import com.tracker.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only REST endpoints.
 *
 * Base path: /api/admin
 *
 * Endpoints:
 *   GET /api/admin/stats            — system-wide KPI statistics
 *   GET /api/admin/activity-logs    — paginated audit trail (all users, newest-first)
 *
 * Security: Spring Security already restricts /api/admin/** to hasRole('ADMIN')
 * via SecurityConfig.  @PreAuthorize provides an additional method-level check.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only system statistics and audit-log endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /api/admin/stats
     *
     * Returns system-wide aggregate statistics used by the Admin Dashboard KPI cards:
     *   totalUsers, activeUsers, totalExpenses, totalIncomes,
     *   totalCategories, totalBudgets, totalExpenseAmount, totalIncomeAmount
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system-wide admin statistics (Admin only)")
    public ResponseEntity<AdminStatsDTO> getAdminStats() {
        return ResponseEntity.ok(adminService.getAdminStats());
    }

    /**
     * GET /api/admin/activity-logs?page=0&size=10
     *
     * Returns a paginated Page<ActivityLogDTO> of ALL activity logs,
     * sorted newest-first.  The frontend uses:
     *   logs.content[]  → log.timestamp, log.username, log.action, log.details
     *   logs.totalPages → for UI.renderPagination()
     */
    @GetMapping("/activity-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get paginated activity logs across all users (Admin only)")
    public ResponseEntity<Page<ActivityLogDTO>> getActivityLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllActivityLogs(page, size));
    }
}
