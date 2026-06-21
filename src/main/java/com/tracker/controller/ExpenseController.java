package com.tracker.controller;

import com.tracker.dto.ExpenseDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.ExpenseService;
import com.tracker.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Manage expenses with CRUD, search, filter, and export")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExportService exportService;

    @PostMapping
    @Operation(summary = "Add a new expense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestBody ExpenseDTO expenseDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(expenseService.addExpense(userId, expenseDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all expenses with pagination")
    public ResponseEntity<Page<ExpenseDTO>> getExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long userId = SecurityUtils.getCurrentUserId();
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(expenseService.getExpensesByUserId(userId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific expense by ID")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(expenseService.getExpenseById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing expense")
    public ResponseEntity<ExpenseDTO> updateExpense(@PathVariable Long id, @RequestBody ExpenseDTO expenseDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(expenseService.updateExpense(userId, id, expenseDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an expense")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        expenseService.deleteExpense(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter expenses dynamically")
    public ResponseEntity<Page<ExpenseDTO>> searchExpenses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long userId = SecurityUtils.getCurrentUserId();
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(expenseService.searchExpenses(userId, keyword, categoryId, startDate, endDate,
                minAmount, maxAmount, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get all expenses by category")
    public ResponseEntity<List<ExpenseDTO>> getByCategory(@PathVariable Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(expenseService.getExpensesByCategory(userId, categoryId));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export expenses to Excel (.xlsx)")
    public ResponseEntity<byte[]> exportToExcel() throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] data = exportService.exportExpensesToExcel(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export expenses to CSV")
    public ResponseEntity<byte[]> exportToCsv() {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] data = exportService.exportExpensesToCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export expenses to PDF report")
    public ResponseEntity<byte[]> exportToPdf() throws Exception {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] data = exportService.exportExpensesToPdf(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
