package com.tracker.controller;

import com.tracker.dto.IncomeDTO;
import com.tracker.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping
    public ResponseEntity<IncomeDTO> addIncome(
            @RequestParam Long userId,
            @Valid @RequestBody IncomeDTO incomeDTO) {
        IncomeDTO response = incomeService.addIncome(userId, incomeDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<IncomeDTO>> getIncomes(@RequestParam Long userId) {
        List<IncomeDTO> response = incomeService.getIncomesByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncomeDTO> getIncomeById(
            @PathVariable Long id,
            @RequestParam Long userId) {
        IncomeDTO response = incomeService.getIncomeById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeDTO> updateIncome(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody IncomeDTO incomeDTO) {
        IncomeDTO response = incomeService.updateIncome(userId, id, incomeDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteIncome(
            @PathVariable Long id,
            @RequestParam Long userId) {
        incomeService.deleteIncome(userId, id);
        return ResponseEntity.ok("Income record deleted successfully.");
    }
}
