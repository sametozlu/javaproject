package com.ecommerce.api.dto.order;

import com.ecommerce.api.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String userEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String couponCode,
        List<OrderItemResponse> items,
        ShippingAddressResponse shippingAddress,
        String trackingNumber,
        Instant createdAt
) {
}
