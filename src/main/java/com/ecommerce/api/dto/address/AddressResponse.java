package com.ecommerce.api.dto.address;

import java.time.Instant;

public record AddressResponse(
        Long id,
        String title,
        String fullName,
        String phone,
        String city,
        String district,
        String addressLine,
        String postalCode,
        boolean defaultAddress,
        Instant createdAt
) {
}
