package com.tracker.controller;

import com.tracker.dto.IncomeDTO;
import com.tracker.service.ExportService;
import com.tracker.service.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Tag(name = "Incomes", description = "Manage income records with CRUD and export")
public class IncomeController {

    private final IncomeService incomeService;
    private final ExportService exportService;

    @PostMapping
    @Operation(summary = "Add a new income record")
    public ResponseEntity<IncomeDTO> addIncome(@RequestParam Long userId, @RequestBody IncomeDTO incomeDTO) {
        return new ResponseEntity<>(incomeService.addIncome(userId, incomeDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all incomes with pagination")
    public ResponseEntity<Page<IncomeDTO>> getIncomes(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(incomeService.getIncomesByUserId(userId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific income by ID")
    public ResponseEntity<IncomeDTO> getIncomeById(@RequestParam Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(incomeService.getIncomeById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing income record")
    public ResponseEntity<IncomeDTO> updateIncome(@RequestParam Long userId, @PathVariable Long id,
                                                   @RequestBody IncomeDTO incomeDTO) {
        return ResponseEntity.ok(incomeService.updateIncome(userId, id, incomeDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an income record")
    public ResponseEntity<Void> deleteIncome(@RequestParam Long userId, @PathVariable Long id) {
        incomeService.deleteIncome(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export incomes to Excel (.xlsx)")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam Long userId) throws IOException {
        byte[] data = exportService.exportIncomesToExcel(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incomes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export incomes to CSV")
    public ResponseEntity<byte[]> exportToCsv(@RequestParam Long userId) {
        byte[] data = exportService.exportIncomesToCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incomes.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}
