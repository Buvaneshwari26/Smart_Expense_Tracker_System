package com.tracker.config;

import com.tracker.model.User;
import com.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializes the default system Admin user:
 * Email: buvaneshwarip6002@gmail.com
 * Username: buvaneshwarip6002
 * Password: Buvana@1712!Secure
 * Role: ADMIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "buvaneshwarip6002@gmail.com";
        String adminUsername = "buvaneshwarip6002";
        String rawPassword = "Buvana@1712!Secure";

        // Demote all other admins to USER so buvaneshwarip6002@gmail.com is the sole initial admin
        List<User> existingAdmins = userRepository.findAll();
        for (User u : existingAdmins) {
            if (!adminEmail.equalsIgnoreCase(u.getEmail()) && "ADMIN".equalsIgnoreCase(u.getRole())) {
                u.setRole("USER");
                userRepository.save(u);
                log.info("Demoted previous admin account '{}' to USER", u.getEmail());
            }
        }

        // Check if primary admin exists
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
            admin -> {
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(rawPassword));
                admin.setRole("ADMIN");
                admin.setAccountActive(true);
                admin.setAccountLocked(false);
                admin.setFailedLoginCount(0);
                userRepository.save(admin);
                log.info("Updated primary Admin user: {}", adminEmail);
            },
            () -> {
                User admin = User.builder()
                        .fullName("Buvaneshwari P")
                        .username(adminUsername)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .role("ADMIN")
                        .currencyPreference("INR")
                        .themePreference("DARK")
                        .notificationPreference(true)
                        .accountActive(true)
                        .accountLocked(false)
                        .failedLoginCount(0)
                        .build();
                userRepository.save(admin);
                log.info("Created primary Admin user: {}", adminEmail);
            }
        );
    }
}
