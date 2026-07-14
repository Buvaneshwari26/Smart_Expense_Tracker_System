package com.tracker.controller;

import com.tracker.dto.ActivityLogDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "Query user activity and system audit logs")
@SecurityRequirement(name = "bearerAuth")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "Search activity logs (Admin sees all; User sees own)")
    public ResponseEntity<Page<ActivityLogDTO>> getActivityLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentRole = SecurityUtils.getCurrentUser().getRole();

        // Enforce security boundaries
        Long targetUserId = userId;
        if (!"ADMIN".equals(currentRole)) {
            targetUserId = currentUserId; // Normal users can only see their own logs
        }

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<ActivityLogDTO> logs = activityLogService.searchLogs(targetUserId, action, keyword, startDate, endDate, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(logs);
    }
}
