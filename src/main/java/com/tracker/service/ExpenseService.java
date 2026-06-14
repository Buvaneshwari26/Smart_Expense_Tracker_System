package com.tracker.service;

import com.tracker.dto.ExpenseDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.Category;
import com.tracker.model.CategoryType;
import com.tracker.model.Expense;
import com.tracker.model.User;
import com.tracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    public ExpenseService(ExpenseRepository expenseRepository, UserService userService, CategoryService categoryService) {
        this.expenseRepository = expenseRepository;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Transactional
    public ExpenseDTO addExpense(Long userId, ExpenseDTO expenseDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(expenseDTO.getCategoryId(), userId);

        // Verify category is of type EXPENSE
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Expense category must be of type EXPENSE. Category " + category.getName() + " is of type " + category.getType());
        }

        Expense expense = Expense.builder()
                .amount(expenseDTO.getAmount())
                .date(expenseDTO.getDate())
                .description(expenseDTO.getDescription())
                .category(category)
                .user(user)
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        return mapToDTO(savedExpense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDTO> getExpensesByUserId(Long userId) {
        userService.getUserEntity(userId);
        return expenseRepository.findByUserId(userId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense record not found with id: " + expenseId + " for user: " + userId));
        return mapToDTO(expense);
    }

    @Transactional
    public ExpenseDTO updateExpense(Long userId, Long expenseId, ExpenseDTO expenseDTO) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense record not found with id: " + expenseId + " for user: " + userId));

        Category category = categoryService.getCategoryEntity(expenseDTO.getCategoryId(), userId);
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Expense category must be of type EXPENSE. Category " + category.getName() + " is of type " + category.getType());
        }

        expense.setAmount(expenseDTO.getAmount());
        expense.setDate(expenseDTO.getDate());
        expense.setDescription(expenseDTO.getDescription());
        expense.setCategory(category);

        Expense updatedExpense = expenseRepository.save(expense);
        return mapToDTO(updatedExpense);
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense record not found with id: " + expenseId + " for user: " + userId));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDTO> filterExpenses(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        userService.getUserEntity(userId);
        
        List<Expense> expenses;
        if (categoryId != null && startDate != null && endDate != null) {
            expenses = expenseRepository.findByUserIdAndCategoryIdAndDateBetween(userId, categoryId, startDate, endDate);
        } else if (categoryId != null) {
            expenses = expenseRepository.findByUserIdAndCategoryId(userId, categoryId);
        } else if (startDate != null && endDate != null) {
            expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        } else {
            expenses = expenseRepository.findByUserId(userId);
        }

        return expenses.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ExpenseDTO mapToDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .description(expense.getDescription())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .build();
    }
}
