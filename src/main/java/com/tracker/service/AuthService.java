package com.tracker.service;

import com.tracker.dto.*;
import com.tracker.exception.BadRequestException;
import com.tracker.model.RefreshToken;
import com.tracker.model.User;
import com.tracker.repository.RefreshTokenRepository;
import com.tracker.repository.UserRepository;
import com.tracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogService activityLogService;
    private final LoginHistoryService loginHistoryService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        validatePasswordStrength(request.getPassword());

        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role("USER")
                .accountLocked(false)
                .accountActive(true)
                .failedLoginCount(0)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        // Log registration
        activityLogService.logActivity(saved, "REGISTER", "User registered successfully");
        loginHistoryService.recordLogin(saved, "SUCCESS_REGISTRATION");

        // Pass userId and role into token so the JWT carries full user context
        String accessToken = jwtTokenProvider.generateTokenFromEmail(saved.getEmail(), saved.getId(), saved.getRole());
        String refreshToken = createRefreshToken(saved);

        return AuthResponse.builder()
                .userId(saved.getId())
                .username(saved.getUsername())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .accessToken(accessToken)
                .token(accessToken)  // alias for Postman compatibility
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .message("User registered successfully")
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getAccountLocked() != null && user.getAccountLocked()) {
            loginHistoryService.recordLogin(user, "FAILED_LOCKED");
            activityLogService.logActivity(user, "LOGIN_FAILED", "Attempted login but account is locked");
            throw new BadRequestException("Account is locked due to multiple failed login attempts. Please contact admin.");
        }

        if (user.getAccountActive() != null && !user.getAccountActive()) {
            loginHistoryService.recordLogin(user, "FAILED_INACTIVE");
            activityLogService.logActivity(user, "LOGIN_FAILED", "Attempted login but account is inactive");
            throw new BadRequestException("Account is inactive. Please contact admin.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // Success: Reset failed count and save last login time
            user.setFailedLoginCount(0);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            loginHistoryService.recordLogin(user, "SUCCESS");
            activityLogService.logActivity(user, "LOGIN", "Successfully logged in");

            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = createRefreshToken(user);

            log.info("User logged in: {}", user.getEmail());

            return AuthResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .accessToken(accessToken)
                    .token(accessToken)  // alias for Postman compatibility
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .message("Login successful")
                    .build();

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            int currentFailed = user.getFailedLoginCount() != null ? user.getFailedLoginCount() : 0;
            currentFailed++;
            user.setFailedLoginCount(currentFailed);
            if (currentFailed >= 5) {
                user.setAccountLocked(true);
                userRepository.save(user);
                loginHistoryService.recordLogin(user, "FAILED_LOCK_TRIGGERED");
                activityLogService.logActivity(user, "ACCOUNT_LOCKED", "Account locked due to 5 failed login attempts");
                throw new BadRequestException("Account locked due to 5 failed attempts. Please contact admin.");
            } else {
                userRepository.save(user);
                loginHistoryService.recordLogin(user, "FAILED");
                activityLogService.logActivity(user, "LOGIN_FAILED", "Failed login attempt (" + currentFailed + "/5)");
                throw new BadRequestException("Invalid email or password. Attempt " + currentFailed + " of 5.");
            }
        } catch (Exception e) {
            loginHistoryService.recordLogin(user, "FAILED");
            activityLogService.logActivity(user, "LOGIN_FAILED", "Login failed: " + e.getMessage());
            throw new BadRequestException("Authentication failed: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token expired. Please login again.");
        }

        User user = token.getUser();
        
        // Secure validation check for refresh token
        if (user.getAccountLocked() != null && user.getAccountLocked()) {
            throw new BadRequestException("User account is locked");
        }
        if (user.getAccountActive() != null && !user.getAccountActive()) {
            throw new BadRequestException("User account is inactive");
        }

        String newAccessToken = jwtTokenProvider.generateTokenFromEmail(user.getEmail(), user.getId(), user.getRole());

        activityLogService.logActivity(user, "REFRESH_TOKEN", "Token refreshed successfully");

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(newAccessToken)
                .token(newAccessToken)  // alias for Postman compatibility
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .message("Token refreshed successfully")
                .build();
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        return refreshTokenRepository.save(refreshToken).getToken();
    }
}
