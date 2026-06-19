package com.tracker.controller;

import com.tracker.dto.SavingsGoalDTO;
import com.tracker.service.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Savings Goals", description = "Manage savings goals with progress tracking")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @PostMapping
    @Operation(summary = "Create a new savings goal")
    public ResponseEntity<SavingsGoalDTO> createGoal(@RequestParam Long userId,
                                                      @RequestBody SavingsGoalDTO goalDTO) {
        return new ResponseEntity<>(savingsGoalService.createGoal(userId, goalDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all savings goals with pagination")
    public ResponseEntity<Page<SavingsGoalDTO>> getGoals(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(savingsGoalService.getGoalsByUserId(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a savings goal by ID")
    public ResponseEntity<SavingsGoalDTO> getGoalById(@RequestParam Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(savingsGoalService.getGoalById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a savings goal")
    public ResponseEntity<SavingsGoalDTO> updateGoal(@RequestParam Long userId, @PathVariable Long id,
                                                      @RequestBody SavingsGoalDTO goalDTO) {
        return ResponseEntity.ok(savingsGoalService.updateGoal(userId, id, goalDTO));
    }

    @PatchMapping("/{id}/add-savings")
    @Operation(summary = "Add or subtract amount from a savings goal")
    public ResponseEntity<SavingsGoalDTO> addSavings(@RequestParam Long userId, @PathVariable Long id,
                                                      @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(savingsGoalService.addSavings(userId, id, amount));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a savings goal")
    public ResponseEntity<Void> deleteGoal(@RequestParam Long userId, @PathVariable Long id) {
        savingsGoalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }
}
