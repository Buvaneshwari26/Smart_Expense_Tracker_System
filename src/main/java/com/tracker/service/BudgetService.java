package com.tracker.service;

import com.tracker.dto.BudgetDTO;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    @Transactional
    public BudgetDTO createBudget(Long userId, BudgetDTO budgetDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);

        Budget budget = Budget.builder()
                .budgetAmount(budgetDTO.getBudgetAmount())
                .month(budgetDTO.getMonth())
                .year(budgetDTO.getYear())
                .category(category)
                .user(user)
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("Budget created: {} for user {}", saved.getId(), userId);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetDTO> getBudgetsByUserId(Long userId) {
        userService.getUserEntity(userId);
        return budgetRepository.findByUserId(userId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetDTO getBudgetById(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));
        return mapToDTO(budget);
    }

    @Transactional
    public BudgetDTO updateBudget(Long userId, Long budgetId, BudgetDTO budgetDTO) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        Category category = categoryService.getCategoryEntity(budgetDTO.getCategoryId(), userId);
        budget.setBudgetAmount(budgetDTO.getBudgetAmount());
        budget.setMonth(budgetDTO.getMonth());
        budget.setYear(budgetDTO.getYear());
        budget.setCategory(category);

        return mapToDTO(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));
        budgetRepository.delete(budget);
        log.info("Budget soft-deleted: {} for user {}", budgetId, userId);
    }

    private BudgetDTO mapToDTO(Budget budget) {
        return BudgetDTO.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .budgetAmount(budget.getBudgetAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .build();
    }
}
