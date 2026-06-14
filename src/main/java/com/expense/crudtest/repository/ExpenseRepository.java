package com.expense.crudtest.repository;

import com.expense.crudtest.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository
        extends JpaRepository<Expense, Long> {
}