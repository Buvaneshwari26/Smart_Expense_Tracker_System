package com.tracker.service;

import com.tracker.dto.UserProfileDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.User;
import com.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for User profile management.
 *
 * Key features:
 * - Duplicate email/username validation (excluding self) on update
 * - BCrypt password encryption
 * - Transactional updates for data consistency
 * - Role assignment (admin use only, enforced at controller)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Long userId) {
        User user = getUserEntity(userId);
        return mapToProfile(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfile)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update profile with duplicate email/username validation.
     * Returns the updated UserProfileDTO so the frontend can sync immediately.
     */
    @Transactional
    public UserProfileDTO updateProfile(Long userId, UserProfileDTO dto) {
        User user = getUserEntity(userId);

        // Validate username uniqueness (ignore current user's own username)
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (userRepository.existsByUsernameAndIdNot(dto.getUsername().trim(), userId)) {
                throw new BadRequestException("Username '" + dto.getUsername() + "' is already taken");
            }
            user.setUsername(dto.getUsername().trim());
        }

        // Validate email uniqueness (ignore current user's own email)
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (userRepository.existsByEmailAndIdNot(dto.getEmail().trim(), userId)) {
                throw new BadRequestException("Email '" + dto.getEmail() + "' is already in use");
            }
            user.setEmail(dto.getEmail().trim());
        }

        // Update non-unique fields
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName().trim());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber().trim());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for userId={}", userId);
        return mapToProfile(saved);
    }

    /**
     * Change password with BCrypt. Validates current password before updating.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters long");
        }
        User user = getUserEntity(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from your current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    /**
     * Soft-delete a user record.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);
        userRepository.delete(user);
        log.info("User deleted: userId={}", userId);
    }

    /**
     * Assign a role to a user. Only ADMIN-callable (enforced at controller layer).
     * Accepts: ADMIN, USER, ANALYST, AUDITOR
     */
    @Transactional
    public UserProfileDTO assignRole(Long userId, String role) {
        String normalizedRole = role.toUpperCase().trim();
        if (!List.of("ADMIN", "USER", "ANALYST", "AUDITOR").contains(normalizedRole)) {
            throw new BadRequestException("Invalid role: " + role + ". Allowed: ADMIN, USER, ANALYST, AUDITOR");
        }
        User user = getUserEntity(userId);
        user.setRole(normalizedRole);
        User saved = userRepository.save(user);
        log.info("Role '{}' assigned to userId={}", normalizedRole, userId);
        return mapToProfile(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapping
    // ─────────────────────────────────────────────────────────────────────────

    private UserProfileDTO mapToProfile(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
