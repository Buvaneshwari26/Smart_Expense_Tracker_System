package com.tracker.service;

import com.tracker.dto.IncomeDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.Category;
import com.tracker.model.CategoryType;
import com.tracker.model.Income;
import com.tracker.model.User;
import com.tracker.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    public IncomeService(IncomeRepository incomeRepository, UserService userService, CategoryService categoryService) {
        this.incomeRepository = incomeRepository;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Transactional
    public IncomeDTO addIncome(Long userId, IncomeDTO incomeDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(incomeDTO.getCategoryId(), userId);

        // Verify category is of type INCOME
        if (category.getType() != CategoryType.INCOME) {
            throw new BadRequestException("Income category must be of type INCOME. Category " + category.getName() + " is of type " + category.getType());
        }

        Income income = Income.builder()
                .amount(incomeDTO.getAmount())
                .date(incomeDTO.getDate())
                .description(incomeDTO.getDescription())
                .category(category)
                .user(user)
                .build();

        Income savedIncome = incomeRepository.save(income);
        return mapToDTO(savedIncome);
    }

    @Transactional(readOnly = true)
    public List<IncomeDTO> getIncomesByUserId(Long userId) {
        userService.getUserEntity(userId);
        return incomeRepository.findByUserId(userId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncomeDTO getIncomeById(Long userId, Long incomeId) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income record not found with id: " + incomeId + " for user: " + userId));
        return mapToDTO(income);
    }

    @Transactional
    public IncomeDTO updateIncome(Long userId, Long incomeId, IncomeDTO incomeDTO) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income record not found with id: " + incomeId + " for user: " + userId));

        Category category = categoryService.getCategoryEntity(incomeDTO.getCategoryId(), userId);
        if (category.getType() != CategoryType.INCOME) {
            throw new BadRequestException("Income category must be of type INCOME. Category " + category.getName() + " is of type " + category.getType());
        }

        income.setAmount(incomeDTO.getAmount());
        income.setDate(incomeDTO.getDate());
        income.setDescription(incomeDTO.getDescription());
        income.setCategory(category);

        Income updatedIncome = incomeRepository.save(income);
        return mapToDTO(updatedIncome);
    }

    @Transactional
    public void deleteIncome(Long userId, Long incomeId) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income record not found with id: " + incomeId + " for user: " + userId));
        incomeRepository.delete(income);
    }

    private IncomeDTO mapToDTO(Income income) {
        return IncomeDTO.builder()
                .id(income.getId())
                .amount(income.getAmount())
                .date(income.getDate())
                .description(income.getDescription())
                .categoryId(income.getCategory().getId())
                .categoryName(income.getCategory().getName())
                .build();
    }
}
