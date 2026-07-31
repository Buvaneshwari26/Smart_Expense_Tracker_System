package com.tracker.service;

import com.tracker.dto.AdminCreateUserRequest;
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
    private final ActivityLogService activityLogService;

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+=-[]{}|;:',.<>?/`~".indexOf(ch) >= 0);

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new BadRequestException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin: Create user with role
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Admin-only user creation. Unlike /auth/register, this allows specifying
     * any role (ADMIN, USER, ANALYST) and full profile fields.
     */
    @Transactional
    public UserProfileDTO adminCreateUser(AdminCreateUserRequest req) {
        validatePasswordStrength(req.getPassword());

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered: " + req.getEmail());
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username already taken: " + req.getUsername());
        }

        String normalizedRole = req.getRole() != null ? req.getRole().toUpperCase().trim() : "USER";
        if (!List.of("ADMIN", "USER", "ANALYST").contains(normalizedRole)) {
            normalizedRole = "USER";
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phoneNumber(req.getPhoneNumber())
                .role(normalizedRole)
                .accountLocked(false)
                .accountActive(true)
                .failedLoginCount(0)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created user: {} with role={}", saved.getEmail(), saved.getRole());
        activityLogService.logActivity(saved, "ADMIN_USER_CREATE", "Admin created user: " + saved.getEmail() + " role=" + saved.getRole());
        return mapToProfile(saved);
    }



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
        if (dto.getProfilePicture() != null) {
            user.setProfilePicture(dto.getProfilePicture());
        }
        if (dto.getCurrencyPreference() != null) {
            user.setCurrencyPreference(dto.getCurrencyPreference());
        }
        if (dto.getThemePreference() != null) {
            user.setThemePreference(dto.getThemePreference());
        }
        if (dto.getNotificationPreference() != null) {
            user.setNotificationPreference(dto.getNotificationPreference());
        }
        
        // Admin-only updates mapped on user via DTO if needed
        if (dto.getAccountActive() != null) {
            user.setAccountActive(dto.getAccountActive());
        }
        if (dto.getAccountLocked() != null) {
            user.setAccountLocked(dto.getAccountLocked());
            if (!dto.getAccountLocked()) {
                user.setFailedLoginCount(0); // Reset count if unlocked
            }
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for userId={}", userId);
        activityLogService.logActivity(saved, "PROFILE_UPDATE", "Updated profile settings");
        return mapToProfile(saved);
    }

    /**
     * Change password with BCrypt. Validates current password before updating.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        validatePasswordStrength(newPassword);

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
        activityLogService.logActivity(user, "PASSWORD_CHANGE", "Password updated successfully");
    }

    /**
     * Soft-delete a user record.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);
        userRepository.delete(user);
        log.info("User deleted: userId={}", userId);
        activityLogService.logActivity(user, "USER_DELETE", "Soft-deleted user account: " + user.getEmail());
    }

    /**
     * Self-delete a user record after password verification.
     */
    @Transactional
    public void deleteOwnAccount(Long userId, String password) {
        User user = getUserEntity(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Incorrect password confirmation");
        }
        userRepository.delete(user);
        log.info("User self-deleted: userId={}", userId);
        activityLogService.logActivity(user, "USER_SELF_DELETE", "Self-deleted user account: " + user.getEmail());
    }

    /**
     * Assign a role to a user. Only ADMIN-callable (enforced at controller layer).
     * Accepts: ADMIN, USER, ANALYST
     */
    @Transactional
    public UserProfileDTO assignRole(Long userId, String role) {
        String normalizedRole = role.toUpperCase().trim();
        if (!List.of("ADMIN", "USER", "ANALYST").contains(normalizedRole)) {
            throw new BadRequestException("Invalid role: " + role + ". Allowed: ADMIN, USER, ANALYST");
        }
        User user = getUserEntity(userId);
        user.setRole(normalizedRole);
        User saved = userRepository.save(user);
        log.info("Role '{}' assigned to userId={}", normalizedRole, userId);
        activityLogService.logActivity(saved, "ROLE_CHANGE", "Assigned role: " + normalizedRole);
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
                .profilePicture(user.getProfilePicture())
                .currencyPreference(user.getCurrencyPreference() != null ? user.getCurrencyPreference() : "INR")
                .themePreference(user.getThemePreference() != null ? user.getThemePreference() : "LIGHT")
                .notificationPreference(user.getNotificationPreference() == null || user.getNotificationPreference())
                .lastLogin(user.getLastLogin())
                .failedLoginCount(user.getFailedLoginCount() != null ? user.getFailedLoginCount() : 0)
                .accountLocked(user.getAccountLocked() != null && user.getAccountLocked())
                .accountActive(user.getAccountActive() == null || user.getAccountActive())
                .build();
    }

    // ── Activate / Deactivate / Unlock ────────────────────────────────────────

    @Transactional
    public UserProfileDTO activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setAccountActive(true);
        user.setAccountLocked(false);
        user.setFailedLoginCount(0);
        User saved = userRepository.save(user);
        activityLogService.logActivity(saved, "USER_ACTIVATED", "Account activated by admin");
        log.info("User activated: {}", saved.getEmail());
        return mapToProfile(saved);
    }

    @Transactional
    public UserProfileDTO deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setAccountActive(false);
        User saved = userRepository.save(user);
        activityLogService.logActivity(saved, "USER_DEACTIVATED", "Account deactivated by admin");
        log.info("User deactivated: {}", saved.getEmail());
        return mapToProfile(saved);
    }

    @Transactional
    public UserProfileDTO unlockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setAccountLocked(false);
        user.setFailedLoginCount(0);
        User saved = userRepository.save(user);
        activityLogService.logActivity(saved, "USER_UNLOCKED", "Account unlocked by admin");
        log.info("User unlocked: {}", saved.getEmail());
        return mapToProfile(saved);
    }

    @Transactional
    public UserProfileDTO lockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setAccountLocked(true);
        User saved = userRepository.save(user);
        activityLogService.logActivity(saved, "USER_LOCKED", "Account locked by admin");
        log.info("User locked: {}", saved.getEmail());
        return mapToProfile(saved);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        validatePasswordStrength(newPassword);
        User user = getUserEntity(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset by admin for userId={}", id);
        activityLogService.logActivity(user, "ADMIN_PASSWORD_RESET", "Password reset by admin");
    }
}
