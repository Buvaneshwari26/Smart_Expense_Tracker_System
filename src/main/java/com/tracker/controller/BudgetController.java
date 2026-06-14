package com.tracker.controller;

import com.tracker.dto.BudgetDTO;
import com.tracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(
            @RequestParam Long userId,
            @Valid @RequestBody BudgetDTO budgetDTO) {
        BudgetDTO response = budgetService.createBudget(userId, budgetDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getBudgets(@RequestParam Long userId) {
        List<BudgetDTO> response = budgetService.getBudgetsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDTO> updateBudget(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody BudgetDTO budgetDTO) {
        BudgetDTO response = budgetService.updateBudget(userId, id, budgetDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBudget(
            @PathVariable Long id,
            @RequestParam Long userId) {
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.ok("Budget limit deleted successfully.");
    }

    @GetMapping("/check")
    public ResponseEntity<String> checkBudgetStatus(
            @RequestParam Long userId,
            @RequestParam Long categoryId) {
        String status = budgetService.checkBudgetStatus(userId, categoryId);
        return ResponseEntity.ok(status);
    }
}
