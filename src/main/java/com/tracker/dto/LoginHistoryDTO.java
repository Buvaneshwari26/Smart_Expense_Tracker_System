package com.tracker.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private LocalDateTime loginAt;
    private String ipAddress;
    private String deviceInfo;
    private String status;
    private String userAgent;
}
