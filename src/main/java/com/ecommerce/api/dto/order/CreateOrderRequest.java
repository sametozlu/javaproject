package com.ecommerce.api.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        String couponCode,
        @NotNull Long addressId
) {
}
