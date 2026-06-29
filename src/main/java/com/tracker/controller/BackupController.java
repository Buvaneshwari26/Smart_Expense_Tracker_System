package com.tracker.controller;

import com.tracker.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
@Tag(name = "Admin Backups", description = "Backup export systems (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class BackupController {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/export")
    @Transactional(readOnly = true)
    @Operation(summary = "Export system database tables as JSON object (Admin only)")
    public ResponseEntity<DatabaseBackupDump> exportDatabase() {
        List<UserBackup> users = userRepository.findAll().stream().map(u -> UserBackup.builder()
                .id(u.getId()).username(u.getUsername()).fullName(u.getFullName())
                .email(u.getEmail()).role(u.getRole()).createdAt(u.getCreatedAt()).build())
                .collect(Collectors.toList());

        List<ExpenseBackup> expenses = expenseRepository.findAll().stream().map(e -> ExpenseBackup.builder()
                .id(e.getId()).amount(e.getAmount()).date(e.getDate())
                .description(e.getDescription())
                .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .createdAt(e.getCreatedAt()).build())
                .collect(Collectors.toList());

        List<IncomeBackup> incomes = incomeRepository.findAll().stream().map(i -> IncomeBackup.builder()
                .id(i.getId()).amount(i.getAmount()).date(i.getDate())
                .source(i.getSource()).description(i.getDescription())
                .categoryId(i.getCategory() != null ? i.getCategory().getId() : null)
                .userId(i.getUser() != null ? i.getUser().getId() : null)
                .createdAt(i.getCreatedAt()).build())
                .collect(Collectors.toList());

        List<CategoryBackup> categories = categoryRepository.findAll().stream().map(c -> CategoryBackup.builder()
                .id(c.getId()).name(c.getName()).type(c.getType() != null ? c.getType().name() : null)
                .description(c.getDescription())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .createdAt(c.getCreatedAt()).build())
                .collect(Collectors.toList());

        DatabaseBackupDump dump = DatabaseBackupDump.builder()
                .users(users).expenses(expenses).incomes(incomes).categories(categories).build();

        return ResponseEntity.ok(dump);
    }

    // ── Nested DTOs for safe serialization ──────────────────────────────────

    @Data @Builder
    public static class DatabaseBackupDump {
        private List<UserBackup> users;
        private List<ExpenseBackup> expenses;
        private List<IncomeBackup> incomes;
        private List<CategoryBackup> categories;
    }

    @Data @Builder
    public static class UserBackup {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String role;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class ExpenseBackup {
        private Long id;
        private BigDecimal amount;
        private LocalDate date;
        private String description;
        private Long categoryId;
        private Long userId;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class IncomeBackup {
        private Long id;
        private BigDecimal amount;
        private LocalDate date;
        private String source;
        private String description;
        private Long categoryId;
        private Long userId;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class CategoryBackup {
        private Long id;
        private String name;
        private String type;
        private String description;
        private Long userId;
        private LocalDateTime createdAt;
    }
}
