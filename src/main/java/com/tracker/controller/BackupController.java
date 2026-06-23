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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
@Tag(name = "Admin Backups", description = "Backup export systems (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/export")
    @Operation(summary = "Export system database tables as JSON object (Admin only)")
    public ResponseEntity<DatabaseBackupDump> exportDatabase() {
        DatabaseBackupDump dump = DatabaseBackupDump.builder()
                .users(userRepository.findAll())
                .expenses(expenseRepository.findAll())
                .incomes(incomeRepository.findAll())
                .categories(categoryRepository.findAll())
                .build();

        return ResponseEntity.ok(dump);
    }

    @Data
    @Builder
    public static class DatabaseBackupDump {
        private List<?> users;
        private List<?> expenses;
        private List<?> incomes;
        private List<?> categories;
    }
}
