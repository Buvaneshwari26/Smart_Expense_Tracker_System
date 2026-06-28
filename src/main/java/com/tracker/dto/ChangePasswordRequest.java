package com.tracker.dto;

import lombok.*;

/**
 * Request body for changing a user's password.
 * Using a dedicated DTO prevents exposing password in query params or URLs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}
