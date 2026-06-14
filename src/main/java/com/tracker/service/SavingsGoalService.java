package com.tracker.service;

import com.tracker.dto.SavingsGoalDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.SavingsGoal;
import com.tracker.model.User;
import com.tracker.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserService userService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository, UserService userService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userService = userService;
    }

    @Transactional
    public SavingsGoalDTO createGoal(Long userId, SavingsGoalDTO goalDTO) {
        User user = userService.getUserEntity(userId);

        if (goalDTO.getTargetDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Target date must be in the future.");
        }

        SavingsGoal goal = SavingsGoal.builder()
                .title(goalDTO.getTitle())
                .targetAmount(goalDTO.getTargetAmount())
                .currentAmount(goalDTO.getCurrentAmount() != null ? goalDTO.getCurrentAmount() : BigDecimal.ZERO)
                .targetDate(goalDTO.getTargetDate())
                .user(user)
                .build();

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        return mapToDTO(savedGoal);
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalDTO> getGoalsByUserId(Long userId) {
        userService.getUserEntity(userId);
        return savingsGoalRepository.findByUserId(userId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public SavingsGoalDTO updateGoal(Long userId, Long goalId, SavingsGoalDTO goalDTO) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId + " for user: " + userId));

        if (goalDTO.getTargetDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Target date must be in the future.");
        }

        goal.setTitle(goalDTO.getTitle());
        goal.setTargetAmount(goalDTO.getTargetAmount());
        goal.setCurrentAmount(goalDTO.getCurrentAmount());
        goal.setTargetDate(goalDTO.getTargetDate());

        SavingsGoal updatedGoal = savingsGoalRepository.save(goal);
        return mapToDTO(updatedGoal);
    }

    @Transactional
    public SavingsGoalDTO updateGoalSavings(Long userId, Long goalId, BigDecimal amountChange) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId + " for user: " + userId));

        BigDecimal newAmount = goal.getCurrentAmount().add(amountChange);
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Savings goal current amount cannot be negative. Current savings: " + goal.getCurrentAmount() + ", attempted change: " + amountChange);
        }

        goal.setCurrentAmount(newAmount);
        SavingsGoal updatedGoal = savingsGoalRepository.save(goal);
        return mapToDTO(updatedGoal);
    }

    @Transactional
    public void deleteGoal(Long userId, Long goalId) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + goalId + " for user: " + userId));
        savingsGoalRepository.delete(goal);
    }

    private SavingsGoalDTO mapToDTO(SavingsGoal goal) {
        return SavingsGoalDTO.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .build();
    }
}
