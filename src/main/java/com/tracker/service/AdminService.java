package com.tracker.service;

import com.tracker.dto.ActivityLogDTO;
import com.tracker.dto.AdminCreateUserRequest;
import com.tracker.dto.AdminStatsDTO;
import com.tracker.dto.UserProfileDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.model.ActivityLog;
import com.tracker.model.User;
import com.tracker.repository.*;
import com.tracker.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for Admin-specific operations:
 *   - System-wide statistics (GET /api/admin/stats)
 *   - Paginated activity logs across all users (GET /api/admin/activity-logs)
 *   - User management CRUD, status toggling, role assignment, lock/unlock, password resets, financial summary, and CSV exports
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository        userRepository;
    private final ExpenseRepository     expenseRepository;
    private final IncomeRepository      incomeRepository;
    private final CategoryRepository    categoryRepository;
    private final BudgetRepository      budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserService           userService;

    // ── Stats ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminStatsDTO getAdminStats() {
        long totalUsers      = userRepository.count();
        long activeUsers     = userRepository.countByAccountActiveTrue();
        long inactiveUsers   = userRepository.countByAccountActiveFalse();
        long adminUsers      = userRepository.countByRole("ADMIN");
        long analystUsers    = userRepository.countByRole("ANALYST");
        long lockedUsers     = userRepository.countByAccountLockedTrue();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);

        long totalExpenses   = expenseRepository.count();
        long totalIncomes    = incomeRepository.count();
        long totalCategories = categoryRepository.count();
        long totalBudgets    = budgetRepository.count();
        long totalSavings    = savingsGoalRepository.count();

        BigDecimal totalExpenseAmount = expenseRepository.sumSystemWideExpense();
        if (totalExpenseAmount == null) totalExpenseAmount = BigDecimal.ZERO;

        BigDecimal totalIncomeAmount = incomeRepository.sumSystemWideIncome();
        if (totalIncomeAmount == null) totalIncomeAmount = BigDecimal.ZERO;

        log.debug("Admin stats: users={}, active={}, inactive={}, admins={}, analysts={}, locked={}",
                totalUsers, activeUsers, inactiveUsers, adminUsers, analystUsers, lockedUsers);

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .adminUsers(adminUsers)
                .analystUsers(analystUsers)
                .lockedUsers(lockedUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalExpenses(totalExpenses)
                .totalIncomes(totalIncomes)
                .totalCategories(totalCategories)
                .totalBudgets(totalBudgets)
                .totalSavingsGoals(totalSavings)
                .totalExpenseAmount(totalExpenseAmount)
                .totalIncomeAmount(totalIncomeAmount)
                .build();
    }

    // ── User Management ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserProfileDTO> searchUsers(int page, int size, String sortParam, String search, String role, Boolean active, Boolean locked) {
        Sort sort = parseSort(sortParam);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = userRepository.searchUsers(
                search != null && !search.isBlank() ? search.trim() : null,
                role != null && !role.isBlank() ? role.trim().toUpperCase() : null,
                active,
                locked,
                pageable
        );
        return users.map(this::mapUserToProfile);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserById(Long id) {
        return userService.getProfile(id);
    }

    @Transactional
    public UserProfileDTO createUser(AdminCreateUserRequest req) {
        return userService.adminCreateUser(req);
    }

    @Transactional
    public UserProfileDTO updateUser(Long id, UserProfileDTO dto) {
        return userService.updateProfile(id, dto);
    }

    @Transactional
    public void deleteUser(Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot delete your own admin account");
        }
        userService.deleteUser(targetUserId);
    }

    @Transactional
    public UserProfileDTO changeRole(Long targetUserId, String newRole) {
        return userService.assignRole(targetUserId, newRole);
    }

    @Transactional
    public UserProfileDTO setStatus(Long targetUserId, boolean active) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!active && currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot deactivate your own admin account");
        }
        return active ? userService.activateUser(targetUserId) : userService.deactivateUser(targetUserId);
    }

    @Transactional
    public UserProfileDTO lockUser(Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot lock your own admin account");
        }
        return userService.lockUser(targetUserId);
    }

    @Transactional
    public UserProfileDTO unlockUser(Long targetUserId) {
        return userService.unlockUser(targetUserId);
    }

    @Transactional
    public void resetPassword(Long targetUserId, String newPassword) {
        userService.resetPassword(targetUserId, newPassword);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserFinancialSummary(Long userId) {
        UserProfileDTO user = userService.getProfile(userId);
        BigDecimal totalIncome = incomeRepository.sumTotalByUserId(userId);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        BigDecimal totalExpense = expenseRepository.sumTotalByUserId(userId);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpense);

        long budgetCount = budgetRepository.countByUserId(userId);
        long goalCount = savingsGoalRepository.countByUserId(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("user", user);
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("balance", balance);
        summary.put("budgetCount", budgetCount);
        summary.put("goalCount", goalCount);
        return summary;
    }

    @Transactional(readOnly = true)
    public byte[] exportUsersCSV() {
        List<User> users = userRepository.findAll(Sort.by("id").ascending());
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Full Name,Username,Email,Phone Number,Role,Account Status,Lock Status,Created At,Last Login\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (User u : users) {
            csv.append(u.getId()).append(",");
            csv.append(escapeCSV(u.getFullName())).append(",");
            csv.append(escapeCSV(u.getUsername())).append(",");
            csv.append(escapeCSV(u.getEmail())).append(",");
            csv.append(escapeCSV(u.getPhoneNumber())).append(",");
            csv.append(u.getRole()).append(",");
            csv.append(Boolean.TRUE.equals(u.getAccountActive()) ? "Active" : "Inactive").append(",");
            csv.append(Boolean.TRUE.equals(u.getAccountLocked()) ? "Locked" : "Unlocked").append(",");
            csv.append(u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "").append(",");
            csv.append(u.getLastLogin() != null ? u.getLastLogin().format(fmt) : "").append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by("id").descending();
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private UserProfileDTO mapUserToProfile(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .profilePicture(user.getProfilePicture())
                .currencyPreference(user.getCurrencyPreference() != null ? user.getCurrencyPreference() : "INR")
                .themePreference(user.getThemePreference() != null ? user.getThemePreference() : "LIGHT")
                .notificationPreference(user.getNotificationPreference() == null || user.getNotificationPreference())
                .lastLogin(user.getLastLogin())
                .failedLoginCount(user.getFailedLoginCount() != null ? user.getFailedLoginCount() : 0)
                .accountLocked(user.getAccountLocked() != null && user.getAccountLocked())
                .accountActive(user.getAccountActive() == null || user.getAccountActive())
                .build();
    }

    // ── Activity Logs ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getAllActivityLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return activityLogRepository.findAll(pageable).map(this::toDTO);
    }

    private ActivityLogDTO toDTO(ActivityLog log) {
        String usernameDisplay = (log.getUser().getUsername() != null && !log.getUser().getUsername().isEmpty())
                ? log.getUser().getUsername()
                : log.getUser().getEmail();

        return ActivityLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .userEmail(log.getUser().getEmail())
                .username(usernameDisplay)
                .action(log.getAction())
                .description(log.getDescription())
                .details(log.getDescription())
                .ipAddress(log.getIpAddress())
                .deviceInfo(log.getDeviceInfo())
                .createdAt(log.getCreatedAt())
                .timestamp(log.getCreatedAt())
                .build();
    }
}
