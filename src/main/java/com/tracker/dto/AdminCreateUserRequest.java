package com.tracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for admin-initiated user creation (POST /api/users).
 * Allows specifying any role (ADMIN, USER, ANALYST) unlike the public /auth/register endpoint.
 */
@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phoneNumber;

    /** Role to assign: ADMIN, USER, or ANALYST. Defaults to USER if null or invalid. */
    private String role = "USER";
}
