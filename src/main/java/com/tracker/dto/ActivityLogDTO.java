package com.tracker.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for ActivityLog records.
 * Fields match what admin.html expects:
 *   log.timestamp  → createdAt alias
 *   log.username   → the user's username or email
 *   log.action     → action type (LOGIN, EXPENSE_CREATED, etc.)
 *   log.details    → human-readable description alias
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogDTO {
    private Long id;
    private Long userId;

    /** User's email address. */
    private String userEmail;

    /**
     * User's display username — used by admin.html as log.username.
     * Populated from User.username or falls back to User.email.
     */
    private String username;

    /** Action type: LOGIN, EXPENSE_CREATED, INCOME_CREATED, etc. */
    private String action;

    /**
     * Human-readable description of the activity.
     * Also exposed as 'details' for the frontend (log.details or log.description).
     */
    private String description;

    /** Alias for description — frontend reads log.details. */
    private String details;

    private String ipAddress;
    private String deviceInfo;

    /**
     * Timestamp of the activity.
     * Frontend reads log.timestamp or log.createdDate — both covered by JSON serialization.
     */
    private LocalDateTime createdAt;

    /**
     * Alias for createdAt — frontend reads log.timestamp.
     * Populated to the same value as createdAt.
     */
    private LocalDateTime timestamp;
}
