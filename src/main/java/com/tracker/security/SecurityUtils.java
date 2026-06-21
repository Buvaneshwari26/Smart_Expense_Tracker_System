package com.tracker.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the currently authenticated user's ID from the SecurityContext.
     * Throws AccessDeniedException if no authenticated user exists.
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }

    /**
     * Returns the currently authenticated user's email.
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        return ((UserPrincipal) authentication.getPrincipal()).getEmail();
    }

    /**
     * Returns the currently authenticated UserPrincipal.
     */
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }
}
