package com.tracker.service;

import com.tracker.dto.LoginHistoryDTO;
import com.tracker.model.LoginHistory;
import com.tracker.model.User;
import com.tracker.repository.LoginHistoryRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional
    public void recordLogin(User user, String status) {
        String ipAddress = getClientIp();
        String userAgent = getClientUserAgent();
        String deviceInfo = getClientDevice(userAgent);

        LoginHistory history = LoginHistory.builder()
                .user(user)
                .loginAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .status(status)
                .userAgent(userAgent)
                .build();

        loginHistoryRepository.save(history);
        log.info("Login recorded: User={} Status={} IP={}", user.getEmail(), status, ipAddress);
    }

    @Transactional(readOnly = true)
    public Page<LoginHistoryDTO> getUserLoginHistory(Long userId, Pageable pageable) {
        return loginHistoryRepository.findByUserId(userId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryDTO> getRecentLoginsForUser(Long userId) {
        return loginHistoryRepository.findTop10ByUserIdOrderByLoginAtDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryDTO> getRecentLogins() {
        return loginHistoryRepository.findTop10ByOrderByLoginAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private LoginHistoryDTO mapToDTO(LoginHistory history) {
        return LoginHistoryDTO.builder()
                .id(history.getId())
                .userId(history.getUser().getId())
                .userEmail(history.getUser().getEmail())
                .loginAt(history.getLoginAt())
                .ipAddress(history.getIpAddress())
                .deviceInfo(history.getDeviceInfo())
                .status(history.getStatus())
                .userAgent(history.getUserAgent())
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

    private String getClientUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown User-Agent";
    }

    private String getClientDevice(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }
        // Extract basic device/OS info
        String lower = userAgent.toLowerCase();
        String os = "Unknown OS";
        if (lower.contains("windows")) os = "Windows";
        else if (lower.contains("macintosh") || lower.contains("mac os")) os = "macOS";
        else if (lower.contains("android")) os = "Android";
        else if (lower.contains("iphone") || lower.contains("ipad")) os = "iOS";
        else if (lower.contains("linux")) os = "Linux";

        String browser = "Unknown Browser";
        if (lower.contains("chrome")) browser = "Chrome";
        else if (lower.contains("firefox")) browser = "Firefox";
        else if (lower.contains("safari")) browser = "Safari";
        else if (lower.contains("edge")) browser = "Edge";
        else if (lower.contains("opera")) browser = "Opera";

        return os + " (" + browser + ")";
    }
}
