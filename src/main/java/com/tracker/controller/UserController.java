package com.tracker.controller;

import com.tracker.dto.ChangePasswordRequest;
import com.tracker.dto.UserProfileDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User management and profile endpoints.
 *
 * RBAC Summary:
 *  GET  /api/users          → ADMIN, AUDITOR
 *  DELETE /api/users/{id}   → ADMIN only
 *  GET  /api/users/{id}     → ADMIN, AUDITOR, or own profile (USER, ANALYST)
 *  PUT  /api/users/{id}     → ADMIN or own profile (USER)
 *  POST /api/users/{id}/change-password → own profile only (USER, ADMIN)
 *  GET  /api/users/profile  → any authenticated user
 *  PUT  /api/users/profile  → USER, ADMIN
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management (Admin/Auditor) and Profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ── Admin + Auditor: list all users ──────────────────────────────────────

    /**
     * GET /api/users — get all users.
     * Accessible by ADMIN (full details) and AUDITOR (read-only compliance view).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get all users (Admin/Auditor only)")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * DELETE /api/users/{id} — soft-delete a user.
     * Accessible by ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a user (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── Current user profile shortcuts ────────────────────────────────────────

    /**
     * GET /api/users/profile — returns the profile of the currently logged-in user.
     * All roles can access.
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user's own profile")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    /**
     * PUT /api/users/profile — update the currently logged-in user's profile.
     * Only USER and ADMIN can update (ANALYST and AUDITOR are read-only).
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update current user's own profile")
    public ResponseEntity<UserProfileDTO> updateMyProfile(@RequestBody UserProfileDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    /**
     * POST /api/users/profile/change-password — change password (body-based for security).
     * Only USER and ADMIN.
     */
    @PostMapping("/profile/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<Map<String, String>> changeMyPassword(@RequestBody ChangePasswordRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ── Admin / self — access by explicit ID ─────────────────────────────────

    /**
     * GET /api/users/{id} — get user by ID.
     * ADMIN and AUDITOR can access any user; USER/ANALYST can only access their own.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Admin/Auditor full access; own profile otherwise)")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUser().getRole();
        // ADMIN and AUDITOR can see any user; others can only see their own
        if (!"ADMIN".equals(role) && !"AUDITOR".equals(role) && !currentUserId.equals(id)) {
            throw new AccessDeniedException("You can only view your own profile");
        }
        return ResponseEntity.ok(userService.getProfile(id));
    }

    /**
     * PUT /api/users/{id} — update user profile by ID.
     * Only ADMIN or the user themselves.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update user profile by ID (Admin or own profile)")
    public ResponseEntity<UserProfileDTO> updateProfile(@PathVariable Long id, @RequestBody UserProfileDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUser().getRole();
        if (!"ADMIN".equals(role) && !currentUserId.equals(id)) {
            throw new AccessDeniedException("You can only update your own profile");
        }
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }

    /**
     * POST /api/users/{id}/change-password — change password by user ID.
     * Only ADMIN or the user themselves can change their password.
     */
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Change password by user ID (own account only)")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUser().getRole();
        if (!"ADMIN".equals(role) && !currentUserId.equals(id)) {
            throw new AccessDeniedException("You can only change your own password");
        }
        userService.changePassword(id, req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * PATCH /api/users/{id}/role — assign role to a user (Admin only).
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to a user (Admin only)")
    public ResponseEntity<UserProfileDTO> assignRole(
            @PathVariable Long id,
            @RequestParam String role) {
        return ResponseEntity.ok(userService.assignRole(id, role));
    }
}
