package com.ecommerce.api.dto.coupon;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank @Size(max = 50) String code,
        @NotNull @Min(1) @Max(100) Integer discountPercent,
        @NotNull @DecimalMin("0") BigDecimal minOrderAmount,
        Integer maxUses,
        Instant expiresAt
) {
}
