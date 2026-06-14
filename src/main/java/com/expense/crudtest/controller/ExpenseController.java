package com.expense.crudtest.controller;

import com.expense.crudtest.entity.Expense;
import com.expense.crudtest.repository.ExpenseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository repository;

    @PostMapping
    public Expense createExpense(@RequestBody Expense expense) {
        return repository.save(expense);
    }

    @GetMapping
    public List<Expense> getAllExpenses() {
        return repository.findAll();
    }

    @PutMapping("/{id}")
    public Expense updateExpense(
            @PathVariable Long id,
            @RequestBody Expense expense) {

        Expense existing = repository.findById(id).orElseThrow();

        existing.setTitle(expense.getTitle());
        existing.setAmount(expense.getAmount());

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public String deleteExpense(@PathVariable Long id) {

        repository.deleteById(id);

        return "Expense Deleted";
    }
}