package com.tracker.controller;

import com.tracker.dto.AdminCreateUserRequest;
import com.tracker.dto.ChangePasswordRequest;
import com.tracker.dto.UserProfileDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 *  GET  /api/users          → ADMIN only
 *  DELETE /api/users/{id}   → ADMIN only
 *  GET  /api/users/{id}     → ADMIN, or own profile (USER, ANALYST)
 *  PUT  /api/users/{id}     → ADMIN or own profile (USER)
 *  POST /api/users/{id}/change-password → own profile only (USER, ADMIN)
 *  GET  /api/users/profile  → any authenticated user
 *  PUT  /api/users/profile  → USER, ADMIN
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management and admin-level user access")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ── Admin: list all users ───────────────────────────────────────────────

    /**
     * GET /api/users — get all users.
     * Accessible by ADMIN (full details).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * POST /api/users — Admin creates a new user with any specified role.
     * Unlike /api/auth/register, this endpoint is ADMIN-only and allows
     * setting any role (ADMIN, USER, ANALYST) at creation time.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin creates a new user with any role")
    public ResponseEntity<UserProfileDTO> adminCreateUser(@Valid @RequestBody AdminCreateUserRequest req) {
        return new ResponseEntity<>(userService.adminCreateUser(req), HttpStatus.CREATED);
    }

    /**
     * DELETE /api/users/profile — self-delete own user account.
     * Accessible by any authenticated user.
     */
    @DeleteMapping("/profile")
    @Operation(summary = "Self-delete own user account")
    public ResponseEntity<Void> deleteOwnAccount(@RequestParam String password) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.deleteOwnAccount(userId, password);
        return ResponseEntity.noContent().build();
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
     * Only USER and ADMIN can update (ANALYST is read-only).
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
     * ADMIN can access any user; USER/ANALYST can only access their own.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Admin full access; own profile otherwise)")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUser().getRole();
        // ADMIN can see any user; others can only see their own
        if (!"ADMIN".equals(role) && !currentUserId.equals(id)) {
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

    /**
     * PATCH /api/users/{id}/activate — activate a user account (Admin only).
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a user account (Admin only)")
    public ResponseEntity<UserProfileDTO> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    /**
     * PATCH /api/users/{id}/deactivate — deactivate a user account (Admin only).
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user account (Admin only)")
    public ResponseEntity<UserProfileDTO> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    /**
     * PATCH /api/users/{id}/unlock — unlock a locked user account (Admin only).
     */
    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock a locked user account (Admin only)")
    public ResponseEntity<UserProfileDTO> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unlockUser(id));
    }
}
