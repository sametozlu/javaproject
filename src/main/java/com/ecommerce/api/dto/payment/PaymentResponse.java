package com.ecommerce.api.dto.payment;

import com.ecommerce.api.domain.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        PaymentStatus status,
        BigDecimal amount,
        String message
) {
}
