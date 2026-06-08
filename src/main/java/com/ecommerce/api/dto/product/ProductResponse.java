package com.ecommerce.api.dto.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        List<String> imageUrls,
        Double averageRating,
        Long reviewCount,
        Instant createdAt
) {
}
