package com.tracker.service;

import com.tracker.dto.ExpenseDTO;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ExpenseService expenseService;

    @Transactional
    public RecurringTransaction createSchedule(Long userId, Double amount, String description, Long categoryId, String frequency, LocalDate startDate) {
        User user = userService.getUserEntity(userId);
        Category category = categoryService.getCategoryEntity(categoryId, userId);

        RecurringTransaction schedule = RecurringTransaction.builder()
                .amount(amount)
                .description(description)
                .category(category)
                .frequency(frequency)
                .startDate(startDate)
                .nextExecutionDate(startDate)
                .user(user)
                .isActive(true)
                .build();

        return recurringTransactionRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<RecurringTransaction> getSchedules(Long userId) {
        return recurringTransactionRepository.findByUserId(userId);
    }

    @Transactional
    public RecurringTransaction updateSchedule(Long userId, Long scheduleId, Double amount, String description, Long categoryId, String frequency, boolean isActive) {
        RecurringTransaction schedule = recurringTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring schedule not found"));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access to schedule");
        }

        Category category = categoryService.getCategoryEntity(categoryId, userId);
        schedule.setAmount(amount);
        schedule.setDescription(description);
        schedule.setCategory(category);
        schedule.setFrequency(frequency);
        schedule.setActive(isActive);

        return recurringTransactionRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long userId, Long scheduleId) {
        RecurringTransaction schedule = recurringTransactionRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring schedule not found"));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access to schedule");
        }

        recurringTransactionRepository.delete(schedule);
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily execution task at 1:00 AM
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> pending = recurringTransactionRepository.findByIsActiveTrueAndNextExecutionDateLessThanEqual(today);
        log.info("Processing {} pending recurring transactions for date: {}", pending.size(), today);

        for (RecurringTransaction t : pending) {
            try {
                // Generate Expense entry
                ExpenseDTO expenseDTO = ExpenseDTO.builder()
                        .amount(java.math.BigDecimal.valueOf(t.getAmount()))
                        .date(t.getNextExecutionDate())
                        .description(t.getDescription())
                        .categoryId(t.getCategory().getId())
                        .build();

                expenseService.addExpense(t.getUser().getId(), expenseDTO);

                // Compute next execution date
                LocalDate nextDate = computeNextDate(t.getNextExecutionDate(), t.getFrequency());
                t.setLastExecutionDate(t.getNextExecutionDate());
                t.setNextExecutionDate(nextDate);
                recurringTransactionRepository.save(t);

                log.info("Processed recurring transaction: {} (next: {})", t.getId(), nextDate);
            } catch (Exception e) {
                log.error("Failed to execute recurring transaction: {}", t.getId(), e);
            }
        }
    }

    private LocalDate computeNextDate(LocalDate current, String frequency) {
        switch (frequency.toUpperCase()) {
            case "DAILY":
                return current.plusDays(1);
            case "WEEKLY":
                return current.plusWeeks(1);
            case "MONTHLY":
            default:
                return current.plusMonths(1);
        }
    }
}
