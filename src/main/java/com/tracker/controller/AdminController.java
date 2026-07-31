package com.tracker.controller;

import com.tracker.dto.ActivityLogDTO;
import com.tracker.dto.AdminCreateUserRequest;
import com.tracker.dto.AdminStatsDTO;
import com.tracker.dto.UserProfileDTO;
import com.tracker.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only REST endpoints.
 * Base path: /api/admin
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only statistics, audit logs, and user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // ── Stats & Activity Logs ────────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system-wide admin statistics (Admin only)")
    public ResponseEntity<AdminStatsDTO> getAdminStats() {
        return ResponseEntity.ok(adminService.getAdminStats());
    }

    @GetMapping("/activity-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get paginated activity logs across all users (Admin only)")
    public ResponseEntity<Page<ActivityLogDTO>> getActivityLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllActivityLogs(page, size));
    }

    // ── User Management Endpoints ─────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get paginated, searchable, filterable list of system users (Admin only)")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean locked) {
        return ResponseEntity.ok(adminService.searchUsers(page, size, sort, search, role, active, locked));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get detailed user profile by ID (Admin only)")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @GetMapping("/users/{id}/financial-summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get detailed financial summary of a specific user (Admin only)")
    public ResponseEntity<Map<String, Object>> getUserFinancialSummary(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserFinancialSummary(id));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user with role (Admin only)")
    public ResponseEntity<UserProfileDTO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user details by ID (Admin only)")
    public ResponseEntity<UserProfileDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserProfileDTO dto) {
        return ResponseEntity.ok(adminService.updateUser(id, dto));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user account by ID (Admin only)")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // Supports both PUT and PATCH for role change
    @RequestMapping(value = "/users/{id}/role", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user (Admin only)")
    public ResponseEntity<UserProfileDTO> changeRole(
            @PathVariable Long id,
            @RequestParam String role) {
        return ResponseEntity.ok(adminService.changeRole(id, role));
    }

    // Supports both PUT and PATCH for active status toggle
    @RequestMapping(value = "/users/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle user active status (Admin only)")
    public ResponseEntity<UserProfileDTO> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(adminService.setStatus(id, active));
    }

    // Supports both PUT and PATCH for lock
    @RequestMapping(value = "/users/{id}/lock", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock user account (Admin only)")
    public ResponseEntity<UserProfileDTO> lockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.lockUser(id));
    }

    // Supports both PUT and PATCH for unlock
    @RequestMapping(value = "/users/{id}/unlock", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user account (Admin only)")
    public ResponseEntity<UserProfileDTO> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockUser(id));
    }

    // Reset password endpoint
    @RequestMapping(value = "/users/{id}/reset-password", method = {RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset user password (Admin only)")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @GetMapping("/users/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export all users as CSV file (Admin only)")
    public ResponseEntity<byte[]> exportUsersCSV() {
        byte[] csvData = adminService.exportUsersCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @Data
    public static class ResetPasswordRequest {
        private String newPassword;
    }
}
