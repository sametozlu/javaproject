package com.ecommerce.api.dto.payment;

public record PaymentConfigResponse(
        boolean stripeEnabled,
        String publishableKey
) {
}
