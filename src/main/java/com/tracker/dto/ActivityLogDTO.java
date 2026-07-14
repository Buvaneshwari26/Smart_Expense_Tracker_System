package com.tracker.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String action;
    private String description;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime createdAt;
}
