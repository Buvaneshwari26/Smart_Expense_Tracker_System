package com.tracker.controller;

import com.tracker.model.RecurringTransaction;
import com.tracker.security.SecurityUtils;
import com.tracker.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
@Tag(name = "Recurring Transactions", description = "Schedules for recurring expenses")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    @Operation(summary = "Create a recurring transaction schedule")
    public ResponseEntity<RecurringTransaction> createSchedule(@RequestBody RecurringRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        return ResponseEntity.ok(recurringTransactionService.createSchedule(
                userId, req.getAmount(), req.getDescription(), req.getCategoryId(), req.getFrequency(), start));
    }

    @GetMapping
    @Operation(summary = "Get list of recurring schedules")
    public ResponseEntity<List<RecurringTransaction>> getSchedules() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(recurringTransactionService.getSchedules(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update schedule info or toggle active state")
    public ResponseEntity<RecurringTransaction> updateSchedule(@PathVariable Long id, @RequestBody RecurringRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(recurringTransactionService.updateSchedule(
                userId, id, req.getAmount(), req.getDescription(), req.getCategoryId(), req.getFrequency(), req.isActive()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a recurring transaction schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        recurringTransactionService.deleteSchedule(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trigger-process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger the processing check of pending schedules (Admin only)")
    public ResponseEntity<String> triggerProcess() {
        recurringTransactionService.processRecurringTransactions();
        return ResponseEntity.ok("Scheduler processed successfully");
    }

    @Data
    public static class RecurringRequest {
        private Double amount;
        private String description;
        private Long categoryId;
        private String frequency; // DAILY, WEEKLY, MONTHLY
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        private boolean active = true;
    }
}
