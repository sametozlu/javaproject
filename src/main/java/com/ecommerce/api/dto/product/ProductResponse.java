package com.ecommerce.api.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String imageUrl,
        Double averageRating,
        Long reviewCount,
        Instant createdAt
) {
}
