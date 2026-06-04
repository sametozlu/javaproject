package com.ecommerce.api.dto.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        String userName,
        Integer rating,
        String comment,
        Instant createdAt
) {
}
