package com.ecommerce.api.dto.order;

import com.ecommerce.api.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}
