package com.ecommerce.api.dto.auth;

import com.ecommerce.api.domain.Role;

public record AuthResponse(
        String token,
        String refreshToken,
        String type,
        Long userId,
        String email,
        String fullName,
        Role role
) {
    public static AuthResponse of(String token, String refreshToken, Long userId, String email, String fullName, Role role) {
        return new AuthResponse(token, refreshToken, "Bearer", userId, email, fullName, role);
    }
}
