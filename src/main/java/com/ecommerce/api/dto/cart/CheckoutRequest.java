package com.ecommerce.api.dto.cart;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        String couponCode,
        @NotNull Long addressId
) {
}
