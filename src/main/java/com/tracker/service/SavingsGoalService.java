package com.tracker.service;

import com.tracker.dto.SavingsGoalDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.*;
import com.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserService userService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ActivityLogService activityLogService;

    @Transactional
    public SavingsGoalDTO createGoal(Long userId, SavingsGoalDTO goalDTO) {
        if (goalDTO == null) {
            throw new BadRequestException("Savings goal request cannot be empty.");
        }
        if (goalDTO.getGoalName() == null || goalDTO.getGoalName().isBlank()) {
            throw new BadRequestException("Goal name is required.");
        }
        if (goalDTO.getTargetAmount() == null || goalDTO.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be greater than zero.");
        }

        User user = userService.getUserEntity(userId);
        BigDecimal currentAmt = goalDTO.getCurrentAmount() != null ? goalDTO.getCurrentAmount() : BigDecimal.ZERO;
        if (currentAmt.compareTo(BigDecimal.ZERO) < 0) {
            currentAmt = BigDecimal.ZERO;
        }

        LocalDate startDate = goalDTO.getStartDate() != null ? goalDTO.getStartDate() : LocalDate.now();
        LocalDate targetDate = goalDTO.getTargetDate() != null ? goalDTO.getTargetDate() : LocalDate.now().plusMonths(1);

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(goalDTO.getGoalName().trim())
                .targetAmount(goalDTO.getTargetAmount())
                .currentAmount(currentAmt)
                .targetDate(targetDate)
                .startDate(startDate)
                .notes(goalDTO.getNotes() != null ? goalDTO.getNotes().trim() : null)
                .user(user)
                .build();

        SavingsGoal saved = savingsGoalRepository.save(goal);
        activityLogService.logActivity(user, "SAVINGS_CREATE", "Created savings goal: " + saved.getGoalName() + " of ₹" + saved.getTargetAmount());
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<SavingsGoalDTO> getGoalsByUserId(Long userId, Pageable pageable) {
        userService.getUserEntity(userId);
        return savingsGoalRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public SavingsGoalDTO getGoalById(Long userId, Long goalId) {
        return mapToDTO(savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId)));
    }

    @Transactional
    public SavingsGoalDTO updateGoal(Long userId, Long goalId, SavingsGoalDTO goalDTO) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId));

        if (goalDTO.getGoalName() != null && !goalDTO.getGoalName().isBlank()) {
            goal.setGoalName(goalDTO.getGoalName().trim());
        }
        if (goalDTO.getTargetAmount() != null && goalDTO.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            goal.setTargetAmount(goalDTO.getTargetAmount());
        }
        if (goalDTO.getCurrentAmount() != null && goalDTO.getCurrentAmount().compareTo(BigDecimal.ZERO) >= 0) {
            goal.setCurrentAmount(goalDTO.getCurrentAmount());
        }
        if (goalDTO.getTargetDate() != null) {
            goal.setTargetDate(goalDTO.getTargetDate());
        }
        if (goalDTO.getStartDate() != null) {
            goal.setStartDate(goalDTO.getStartDate());
        }
        if (goalDTO.getNotes() != null) {
            goal.setNotes(goalDTO.getNotes().trim());
        }

        SavingsGoal saved = savingsGoalRepository.save(goal);
        activityLogService.logActivity(saved.getUser(), "SAVINGS_UPDATE", "Updated savings goal: " + saved.getGoalName());
        return mapToDTO(saved);
    }

    @Transactional
    public SavingsGoalDTO addSavings(Long userId, Long goalId, BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Amount is required.");
        }
        User user = userService.getUserEntity(userId);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId));

        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal newAmount = current.add(amount);
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Savings cannot go below zero.");
        }
        goal.setCurrentAmount(newAmount);
        SavingsGoal saved = savingsGoalRepository.save(goal);

        activityLogService.logActivity(user, "SAVINGS_ADD", "Added ₹" + amount + " to savings goal: " + saved.getGoalName());

        // Notify if goal achieved
        if (saved.getTargetAmount() != null && saved.getCurrentAmount().compareTo(saved.getTargetAmount()) >= 0) {
            try {
                Notification notification = Notification.builder()
                        .title("🎉 Savings Goal Achieved!")
                        .message("Congratulations! You have achieved your savings goal: " + saved.getGoalName())
                        .user(user)
                        .isRead(false)
                        .build();
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.warn("Failed to create notification: {}", e.getMessage());
            }
            emailService.sendSavingsGoalAchievedAlert(user.getEmail(), user.getUsername(),
                    saved.getGoalName(), saved.getTargetAmount());
        }
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteGoal(Long userId, Long goalId) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId));
        savingsGoalRepository.delete(goal);
        activityLogService.logActivity(goal.getUser(), "SAVINGS_DELETE", "Deleted savings goal: " + goal.getGoalName());
    }

    private SavingsGoalDTO mapToDTO(SavingsGoal goal) {
        BigDecimal targetAmt = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal currentAmt = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;

        BigDecimal pct = targetAmt.compareTo(BigDecimal.ZERO) > 0
                ? currentAmt.multiply(BigDecimal.valueOf(100)).divide(targetAmt, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Estimate Completion Date
        String estCompletion = "No savings recorded yet";
        if (targetAmt.compareTo(BigDecimal.ZERO) > 0 && currentAmt.compareTo(targetAmt) >= 0) {
            estCompletion = "Goal Achieved";
        } else if (currentAmt.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate createdDate = goal.getCreatedAt() != null ? goal.getCreatedAt().toLocalDate() : LocalDate.now().minusDays(1);
            long days = java.time.temporal.ChronoUnit.DAYS.between(createdDate, LocalDate.now());
            if (days <= 0) days = 1;
            BigDecimal savingsRatePerDay = currentAmt.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
            if (savingsRatePerDay.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal remaining = targetAmt.subtract(currentAmt);
                BigDecimal daysNeeded = remaining.divide(savingsRatePerDay, 0, RoundingMode.CEILING);
                estCompletion = LocalDate.now().plusDays(daysNeeded.longValue()).toString();
            } else {
                estCompletion = "No savings velocity";
            }
        }

        BigDecimal remainingAmount = targetAmt.subtract(currentAmt);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        String status = pct.compareTo(BigDecimal.valueOf(100)) >= 0 ? "Achieved" : "In Progress";

        return SavingsGoalDTO.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(targetAmt)
                .currentAmount(currentAmt)
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate() != null ? goal.getStartDate() : (goal.getCreatedAt() != null ? goal.getCreatedAt().toLocalDate() : LocalDate.now()))
                .notes(goal.getNotes())
                .percentage(pct)
                .remainingAmount(remainingAmount)
                .status(status)
                .estimatedCompletionDate(estCompletion)
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
