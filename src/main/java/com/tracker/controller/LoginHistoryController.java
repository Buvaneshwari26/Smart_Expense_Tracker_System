package com.tracker.controller;

import com.tracker.dto.LoginHistoryDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.LoginHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login-history")
@RequiredArgsConstructor
@Tag(name = "Login History", description = "Query user authentication history and logins")
@SecurityRequirement(name = "bearerAuth")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping
    @Operation(summary = "Get login history (Admin can filter by userId; User sees own)")
    public ResponseEntity<Page<LoginHistoryDTO>> getLoginHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "loginAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentRole = SecurityUtils.getCurrentUser().getRole();

        Long targetUserId = userId;
        if (!"ADMIN".equals(currentRole)) {
            targetUserId = currentUserId; // Normal users can only see their own login history
        } else if (targetUserId == null) {
            // For admin, if no specific user filter, default to showing their own or we can just require a userId,
            // let's default to currentUserId if targetUserId is null, or support get all (let's default to all if admin passes null).
            // Wait, does JpaRepository allow finding all if userId is null? We can write service support,
            // but normally normal user gets currentUserId. Let's make it targetUserId.
        }

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<LoginHistoryDTO> history;
        if (targetUserId == null && "ADMIN".equals(currentRole)) {
            // Admin gets all login histories
            history = loginHistoryService.getUserLoginHistory(null, PageRequest.of(page, size, sort));
        } else {
            if (targetUserId == null) {
                targetUserId = currentUserId;
            }
            history = loginHistoryService.getUserLoginHistory(targetUserId, PageRequest.of(page, size, sort));
        }
        return ResponseEntity.ok(history);
    }
}
