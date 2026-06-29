package com.tracker.controller;

import com.tracker.dto.ApiResponse;
import com.tracker.model.User;
import com.tracker.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Bulk Registration", description = "Endpoints for batch operations and setups")
public class AdminRegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/pre-register-bulk")
    @Operation(summary = "Pre-register specific users with explicit roles and passwords")
    public ResponseEntity<ApiResponse<List<RegistrationResult>>> preRegisterBulk() {
        List<PreRegUser> targetUsers = List.of(
            PreRegUser.builder().email("buvaneshwarip6002@gmail.com").username("buvaneshwarip6002").fullName("Buvaneshwari P").role("ADMIN").build(),
            PreRegUser.builder().email("buvanap1712@gmail.com").username("buvanap1712").fullName("Buvana P").role("USER").build(),
            PreRegUser.builder().email("buvanesh6421@gmail.com").username("buvanesh6421").fullName("Buvanesh").role("ANALYST").build(),
            PreRegUser.builder().email("2k23cse021@kiot.ac.in").username("student_cse21").fullName("KIOT CSE Student").role("AUDITOR").build()
        );

        String strongPassword = "Buvana@1712!Secure";
        String encodedPassword = passwordEncoder.encode(strongPassword);
        List<RegistrationResult> results = new ArrayList<>();

        for (PreRegUser target : targetUsers) {
            java.util.Optional<User> existingUserOpt = userRepository.findByEmail(target.getEmail());
            if (existingUserOpt.isPresent()) {
                User user = existingUserOpt.get();
                user.setPassword(encodedPassword);
                user.setRole(target.getRole());
                user.setUsername(target.getUsername());
                user.setFullName(target.getFullName());
                userRepository.save(user);
                log.info("Bulk reset user success: {} as {}", user.getEmail(), user.getRole());
                results.add(RegistrationResult.builder()
                        .email(target.getEmail())
                        .status("UPDATED")
                        .role(target.getRole())
                        .build());
            } else {
                User user = User.builder()
                        .fullName(target.getFullName())
                        .username(target.getUsername())
                        .email(target.getEmail())
                        .password(encodedPassword)
                        .role(target.getRole())
                        .build();
                User saved = userRepository.save(user);
                log.info("Bulk registration success: {} as {}", saved.getEmail(), saved.getRole());
                results.add(RegistrationResult.builder()
                        .email(saved.getEmail())
                        .status("CREATED")
                        .role(saved.getRole())
                        .build());
            }
        }

        return ResponseEntity.ok(ApiResponse.<List<RegistrationResult>>builder()
                .success(true)
                .message("Bulk registration execution completed")
                .data(results)
                .build());
    }

    @Data
    @Builder
    private static class PreRegUser {
        private String email;
        private String username;
        private String fullName;
        private String role;
    }

    @Data
    @Builder
    public static class RegistrationResult {
        private String email;
        private String status;
        private String role;
    }
}
