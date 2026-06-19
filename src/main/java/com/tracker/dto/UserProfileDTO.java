package com.tracker.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;
    private LocalDateTime createdAt;
}
