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

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role("USER")
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        // Pass userId and role into token so the JWT carries full user context
        String accessToken = jwtTokenProvider.generateTokenFromEmail(saved.getEmail(), saved.getId(), saved.getRole());
        String refreshToken = createRefreshToken(saved);

        return AuthResponse.builder()
                .userId(saved.getId())
                .username(saved.getUsername())
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        // generateToken from Authentication uses UserPrincipal which has userId+role
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .token(accessToken)  // alias for Postman compatibility
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .message("Login successful")
                .build();
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
        String newAccessToken = jwtTokenProvider.generateTokenFromEmail(user.getEmail(), user.getId(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
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
