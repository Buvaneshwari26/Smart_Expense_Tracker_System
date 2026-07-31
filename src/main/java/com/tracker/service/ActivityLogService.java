package com.tracker.service;

import com.tracker.dto.ActivityLogDTO;
import com.tracker.model.ActivityLog;
import com.tracker.model.User;
import com.tracker.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void logActivity(User user, String action, String description) {
        String ipAddress = getClientIp();
        String deviceInfo = getClientDevice();

        ActivityLog logEntry = ActivityLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .build();

        activityLogRepository.save(logEntry);
        log.info("Activity logged: User={} Action={} Desc={}", user.getEmail(), action, description);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getUserLogs(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> searchLogs(Long userId, String action, String keyword,
                                           LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.searchLogs(userId, action, keyword, startDate, endDate, pageable)
                .map(this::mapToDTO);
    }

    private ActivityLogDTO mapToDTO(ActivityLog log) {
        String usernameDisplay = (log.getUser().getUsername() != null
                && !log.getUser().getUsername().isEmpty())
                ? log.getUser().getUsername()
                : log.getUser().getEmail();

        return ActivityLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .userEmail(log.getUser().getEmail())
                .username(usernameDisplay)          // frontend: log.username
                .action(log.getAction())
                .description(log.getDescription())
                .details(log.getDescription())      // frontend: log.details
                .ipAddress(log.getIpAddress())
                .deviceInfo(log.getDeviceInfo())
                .createdAt(log.getCreatedAt())
                .timestamp(log.getCreatedAt())      // frontend: log.timestamp
                .build();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // Ignore
        }
        return "127.0.0.1";
    }

    private String getClientDevice() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 255) {
                    return userAgent.substring(0, 255);
                }
                return userAgent != null ? userAgent : "Unknown Device";
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown Device";
    }
}
