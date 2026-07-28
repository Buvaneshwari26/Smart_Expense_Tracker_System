package com.tracker.controller;

import com.tracker.dto.BudgetDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management and tracking")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create a new budget (USER and ADMIN only)")
    public ResponseEntity<BudgetDTO> createBudget(@RequestBody BudgetDTO budgetDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(budgetService.createBudget(userId, budgetDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all budgets for the authenticated user")
    public ResponseEntity<List<BudgetDTO>> getBudgets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(filterBudgets(budgetService.getBudgetsByUserId(userId), year, month));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all budgets for a user by user ID")
    public ResponseEntity<List<BudgetDTO>> getBudgetsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(filterBudgets(budgetService.getBudgetsByUserId(userId), year, month));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a budget by ID")
    public ResponseEntity<BudgetDTO> getBudgetById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getBudgetById(userId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update a budget (USER and ADMIN only)")
    public ResponseEntity<BudgetDTO> updateBudget(@PathVariable Long id, @RequestBody BudgetDTO budgetDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.updateBudget(userId, id, budgetDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Soft-delete a budget (USER and ADMIN only)")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }

    private List<BudgetDTO> filterBudgets(List<BudgetDTO> list, Integer year, Integer month) {
        if (list == null) return List.of();
        return list.stream()
                .filter(b -> year == null || (b.getYear() != null && b.getYear().equals(year)))
                .filter(b -> month == null || (b.getMonth() != null && b.getMonth().equals(month)))
                .collect(Collectors.toList());
    }
}
