package com.tracker.controller;

import com.tracker.dto.IncomeDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.ExportService;
import com.tracker.service.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Tag(name = "Incomes", description = "Manage income records with CRUD and export")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    private final IncomeService incomeService;
    private final ExportService exportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Add a new income record (USER and ADMIN only)")
    public ResponseEntity<IncomeDTO> addIncome(@RequestBody IncomeDTO incomeDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(incomeService.addIncome(userId, incomeDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all incomes with pagination")
    public ResponseEntity<Page<IncomeDTO>> getIncomes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long userId = SecurityUtils.getCurrentUserId();
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(incomeService.getIncomesByUserId(userId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific income by ID")
    public ResponseEntity<IncomeDTO> getIncomeById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(incomeService.getIncomeById(userId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update an existing income record (USER and ADMIN only)")
    public ResponseEntity<IncomeDTO> updateIncome(@PathVariable Long id, @RequestBody IncomeDTO incomeDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(incomeService.updateIncome(userId, id, incomeDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Soft-delete an income record (USER and ADMIN only)")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        incomeService.deleteIncome(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export incomes to Excel (.xlsx)")
    public ResponseEntity<byte[]> exportToExcel() throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] data = exportService.exportIncomesToExcel(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incomes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export incomes to CSV")
    public ResponseEntity<byte[]> exportToCsv() {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] data = exportService.exportIncomesToCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incomes.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter incomes dynamically")
    public ResponseEntity<Page<IncomeDTO>> searchIncomes(
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
        return ResponseEntity.ok(incomeService.searchIncomes(userId, keyword, categoryId, startDate, endDate,
                minAmount, maxAmount, PageRequest.of(page, size, sort)));
    }
}
