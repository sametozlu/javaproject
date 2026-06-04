package com.ecommerce.api.dto.payment;

import java.math.BigDecimal;

public record StripePaymentIntentResponse(
        String clientSecret,
        String paymentIntentId,
        BigDecimal amount,
        String currency
) {
}
