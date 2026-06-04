package com.ecommerce.api.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String fullName,
        @Size(min = 6, max = 100) String newPassword,
        String currentPassword
) {
}
