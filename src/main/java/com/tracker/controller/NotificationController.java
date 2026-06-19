package com.tracker.controller;

import com.tracker.model.Notification;
import com.tracker.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Budget and savings goal alerts")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "Get all notifications for a user (paginated)")
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count for a user")
    public ResponseEntity<Long> getUnreadCount(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @PatchMapping("/{id}/mark-read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok().build();
    }
}
