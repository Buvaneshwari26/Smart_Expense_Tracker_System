package com.tracker.controller;

import com.tracker.dto.SavingsGoalDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Savings Goals", description = "Manage savings goals with progress tracking")
@SecurityRequirement(name = "bearerAuth")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create a new savings goal (USER and ADMIN only)")
    public ResponseEntity<SavingsGoalDTO> createGoal(@RequestBody SavingsGoalDTO goalDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(savingsGoalService.createGoal(userId, goalDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all savings goals with pagination")
    public ResponseEntity<Page<SavingsGoalDTO>> getGoals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(savingsGoalService.getGoalsByUserId(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a savings goal by ID")
    public ResponseEntity<SavingsGoalDTO> getGoalById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(savingsGoalService.getGoalById(userId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update a savings goal (USER and ADMIN only)")
    public ResponseEntity<SavingsGoalDTO> updateGoal(@PathVariable Long id, @RequestBody SavingsGoalDTO goalDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(savingsGoalService.updateGoal(userId, id, goalDTO));
    }

    @PatchMapping("/{id}/add-savings")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Add or subtract amount from a savings goal (USER and ADMIN only)")
    public ResponseEntity<SavingsGoalDTO> addSavings(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(savingsGoalService.addSavings(userId, id, amount));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Soft-delete a savings goal (USER and ADMIN only)")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        savingsGoalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }
}
