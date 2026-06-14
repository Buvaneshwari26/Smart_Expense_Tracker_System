package com.tracker.controller;

import com.tracker.dto.SavingsGoalDTO;
import com.tracker.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    public ResponseEntity<SavingsGoalDTO> createGoal(
            @RequestParam Long userId,
            @Valid @RequestBody SavingsGoalDTO goalDTO) {
        SavingsGoalDTO response = savingsGoalService.createGoal(userId, goalDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalDTO>> getGoals(@RequestParam Long userId) {
        List<SavingsGoalDTO> response = savingsGoalService.getGoalsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalDTO> updateGoal(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody SavingsGoalDTO goalDTO) {
        SavingsGoalDTO response = savingsGoalService.updateGoal(userId, id, goalDTO);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/savings")
    public ResponseEntity<SavingsGoalDTO> updateGoalSavings(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam BigDecimal amountChange) {
        SavingsGoalDTO response = savingsGoalService.updateGoalSavings(userId, id, amountChange);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGoal(
            @PathVariable Long id,
            @RequestParam Long userId) {
        savingsGoalService.deleteGoal(userId, id);
        return ResponseEntity.ok("Savings goal deleted successfully.");
    }
}
