package com.tracker.controller;

import com.tracker.dto.BudgetDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management and tracking")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a new budget")
    public ResponseEntity<BudgetDTO> createBudget(@RequestBody BudgetDTO budgetDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(budgetService.createBudget(userId, budgetDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all budgets for the authenticated user")
    public ResponseEntity<List<BudgetDTO>> getBudgets() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getBudgetsByUserId(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a budget by ID")
    public ResponseEntity<BudgetDTO> getBudgetById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getBudgetById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget")
    public ResponseEntity<BudgetDTO> updateBudget(@PathVariable Long id, @RequestBody BudgetDTO budgetDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.updateBudget(userId, id, budgetDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a budget")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }
}
