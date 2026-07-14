package com.tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Column(name = "profile_picture", columnDefinition = "LONGTEXT")
    private String profilePicture;

    @Column(name = "currency_preference", length = 10)
    @Builder.Default
    private String currencyPreference = "INR";

    @Column(name = "theme_preference", length = 10)
    @Builder.Default
    private String themePreference = "LIGHT";

    @Column(name = "notification_preference")
    @Builder.Default
    private Boolean notificationPreference = true;

    @Column(name = "last_login")
    private java.time.LocalDateTime lastLogin;

    @Column(name = "failed_login_count")
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "account_locked")
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "account_active")
    @Builder.Default
    private Boolean accountActive = true;
}
