package com.tracker.service;

import com.tracker.dto.IncomeDTO;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    @Transactional
    public IncomeDTO addIncome(Long userId, IncomeDTO incomeDTO) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(incomeDTO.getCategoryId(), userId);

        Income income = Income.builder()
                .source(incomeDTO.getSource())
                .amount(incomeDTO.getAmount())
                .date(incomeDTO.getDate() != null ? incomeDTO.getDate() : LocalDate.now())
                .description(incomeDTO.getDescription())
                .category(category)
                .user(user)
                .build();

        Income saved = incomeRepository.save(income);
        log.info("Income added: {} for user {}", saved.getId(), userId);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncomeDTO> getIncomesByUserId(Long userId, Pageable pageable) {
        userService.getUserEntity(userId);
        return incomeRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public IncomeDTO getIncomeById(Long userId, Long incomeId) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + incomeId));
        return mapToDTO(income);
    }

    @Transactional
    public IncomeDTO updateIncome(Long userId, Long incomeId, IncomeDTO incomeDTO) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + incomeId));

        Category category = categoryService.getCategoryEntity(incomeDTO.getCategoryId(), userId);
        income.setSource(incomeDTO.getSource());
        income.setAmount(incomeDTO.getAmount());
        income.setDate(incomeDTO.getDate());
        income.setDescription(incomeDTO.getDescription());
        income.setCategory(category);

        return mapToDTO(incomeRepository.save(income));
    }

    @Transactional
    public void deleteIncome(Long userId, Long incomeId) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + incomeId));
        incomeRepository.delete(income);
        log.info("Income soft-deleted: {} for user {}", incomeId, userId);
    }

    public IncomeDTO mapToDTO(Income income) {
        return IncomeDTO.builder()
                .id(income.getId())
                .source(income.getSource())
                .amount(income.getAmount())
                .date(income.getDate())
                .description(income.getDescription())
                .categoryId(income.getCategory().getId())
                .categoryName(income.getCategory().getName())
                .createdAt(income.getCreatedAt())
                .build();
    }

    public List<Income> getIncomeEntitiesByUserId(Long userId) {
        return incomeRepository.findByUserId(userId);
    }
}
