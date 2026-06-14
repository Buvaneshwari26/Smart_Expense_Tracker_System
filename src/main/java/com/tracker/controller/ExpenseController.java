package com.tracker.controller;

import com.tracker.dto.ExpenseDTO;
import com.tracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseDTO> addExpense(
            @RequestParam Long userId,
            @Valid @RequestBody ExpenseDTO expenseDTO) {
        ExpenseDTO response = expenseService.addExpense(userId, expenseDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getExpenses(@RequestParam Long userId) {
        List<ExpenseDTO> response = expenseService.getExpensesByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(
            @PathVariable Long id,
            @RequestParam Long userId) {
        ExpenseDTO response = expenseService.getExpenseById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody ExpenseDTO expenseDTO) {
        ExpenseDTO response = expenseService.updateExpense(userId, id, expenseDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(
            @PathVariable Long id,
            @RequestParam Long userId) {
        expenseService.deleteExpense(userId, id);
        return ResponseEntity.ok("Expense record deleted successfully.");
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ExpenseDTO>> filterExpenses(
            @RequestParam Long userId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseDTO> response = expenseService.filterExpenses(userId, categoryId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
