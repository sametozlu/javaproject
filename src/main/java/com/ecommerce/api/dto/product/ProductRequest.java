package com.ecommerce.api.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull Long categoryId,
        @Min(0) Integer lowStockThreshold
) {
}
