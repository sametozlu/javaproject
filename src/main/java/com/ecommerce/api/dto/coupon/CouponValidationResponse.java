package com.ecommerce.api.dto.coupon;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String code,
        int discountPercent,
        BigDecimal discountAmount,
        String message
) {
}
