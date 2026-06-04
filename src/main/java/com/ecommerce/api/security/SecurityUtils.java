package com.ecommerce.api.security;

import com.ecommerce.api.domain.Role;
import com.ecommerce.api.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Authentication required");
        }
        return principal;
    }

    public static void requireAdmin() {
        if (currentUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
    }
}
