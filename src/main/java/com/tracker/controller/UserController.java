package com.tracker.controller;

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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management (Admin) and Profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ── Admin-only endpoints ──────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a user (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── Profile endpoints — any authenticated user accessing their own profile ─

    /**
     * GET /api/users/profile — returns the profile of the currently logged-in user.
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    /**
     * PUT /api/users/profile — update the currently logged-in user's profile.
     */
    @PutMapping("/profile")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<UserProfileDTO> updateMyProfile(@RequestBody UserProfileDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    /**
     * PATCH /api/users/profile/change-password — change password for the currently logged-in user.
     */
    @PatchMapping("/profile/change-password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<Map<String, String>> changeMyPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ── Admin or self — access by ID ─────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Admin or own profile)")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUser().getRole();
        if (!"ADMIN".equals(role) && !currentUserId.equals(id)) {
            throw new AccessDeniedException("You can only view your own profile");
        }
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile by ID (Admin or own profile)")
    public ResponseEntity<UserProfileDTO> updateProfile(@PathVariable Long id, @RequestBody UserProfileDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUser().getRole();
        if (!"ADMIN".equals(role) && !currentUserId.equals(id)) {
            throw new AccessDeniedException("You can only update your own profile");
        }
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }

    @PatchMapping("/{id}/change-password")
    @Operation(summary = "Change password by user ID (own account only)")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(id)) {
            throw new AccessDeniedException("You can only change your own password");
        }
        userService.changePassword(id, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
