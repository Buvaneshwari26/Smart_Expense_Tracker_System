package com.tracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String action; // LOGIN, LOGOUT, PROFILE_UPDATE, PASSWORD_CHANGE, INCOME_CRUD, EXPENSE_CRUD, etc.

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
