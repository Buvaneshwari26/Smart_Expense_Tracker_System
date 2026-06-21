package com.tracker.controller;

import com.tracker.model.Notification;
import com.tracker.repository.NotificationRepository;
import com.tracker.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Budget and savings goal alerts")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "Get all notifications for the authenticated user (paginated)")
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count for the authenticated user")
    public ResponseEntity<Long> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @PatchMapping("/{id}/mark-read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.findById(id).ifPresent(n -> {
            // Ownership check: only mark as read if it belongs to the authenticated user
            if (n.getUser() != null && n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.ok().build();
    }
}
