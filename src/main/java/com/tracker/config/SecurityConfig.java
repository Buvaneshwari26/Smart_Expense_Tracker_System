package com.tracker.config;

import com.tracker.security.JwtAuthenticationFilter;
import com.tracker.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Role-Based Access Control (RBAC).
 *
 * Roles:
 *  ADMIN   — Full system access (CRUD on everything, user management)
 *  USER    — Own data only (CRUD on own profile, expenses, income, budgets, goals)
 *  ANALYST — Read-only (dashboard, reports, expenses, incomes, budgets, categories)
 *  AUDITOR — Read-only (all users, all transactions, all reports, audit logs)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Enables @PreAuthorize / @PostAuthorize on method level
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints ─────────────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/*.html", "/css/**", "/js/**", "/images/**", "/").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── User Management: ADMIN full access, AUDITOR read-only ────────────
                .requestMatchers(HttpMethod.GET,    "/api/users").hasAnyRole("ADMIN", "AUDITOR")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "AUDITOR")

                // ── Profile: any authenticated user can read/update their own profile ─
                .requestMatchers(HttpMethod.GET,    "/api/users/{id}").authenticated()
                .requestMatchers(HttpMethod.PUT,    "/api/users/{id}").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST,   "/api/users/{id}/change-password").hasAnyRole("ADMIN", "USER")

                // ── Dashboard & Reports: all authenticated roles (read-only routes) ──
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reports/**").authenticated()

                // ── Expenses: ANALYST + AUDITOR = read-only; USER + ADMIN = full CRUD ──
                .requestMatchers(HttpMethod.GET,    "/api/expenses/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/expenses/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,    "/api/expenses/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/expenses/**").hasAnyRole("ADMIN", "USER")

                // ── Incomes: ANALYST + AUDITOR = read-only ──────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/incomes/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/incomes/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,    "/api/incomes/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/incomes/**").hasAnyRole("ADMIN", "USER")

                // ── Categories: ANALYST + AUDITOR = read-only ───────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/categories/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/categories/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,    "/api/categories/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                // ── Budgets: ANALYST + AUDITOR = read-only ──────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/budgets/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/budgets/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,    "/api/budgets/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/budgets/**").hasAnyRole("ADMIN", "USER")

                // ── Savings Goals: ANALYST + AUDITOR = read-only ────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/savings-goals/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/savings-goals/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,    "/api/savings-goals/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PATCH,  "/api/savings-goals/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/savings-goals/**").hasAnyRole("ADMIN", "USER")

                // ── Recurring Transactions: USER + ADMIN only ────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/recurring/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST,   "/api/recurring/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,    "/api/recurring/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/recurring/**").hasAnyRole("ADMIN", "USER")

                // ── Notifications: all authenticated roles ───────────────────────────
                .requestMatchers("/api/notifications/**").authenticated()

                // ── Catch-all: must be authenticated ────────────────────────────────
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
