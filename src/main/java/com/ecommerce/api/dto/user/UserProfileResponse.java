package com.ecommerce.api.dto.user;

import com.ecommerce.api.domain.Role;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Instant createdAt,
        long orderCount
) {
}
