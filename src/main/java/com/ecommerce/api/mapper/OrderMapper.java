package com.ecommerce.api.mapper;

import com.ecommerce.api.domain.Order;
import com.ecommerce.api.domain.OrderItem;
import com.ecommerce.api.dto.order.OrderItemResponse;
import com.ecommerce.api.dto.order.OrderResponse;
import com.ecommerce.api.dto.order.ShippingAddressResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO,
                order.getCouponCode(),
                items,
                toShipping(order),
                order.getTrackingNumber(),
                order.getCreatedAt()
        );
    }

    private ShippingAddressResponse toShipping(Order order) {
        if (order.getShippingFullName() == null) {
            return null;
        }
        return new ShippingAddressResponse(
                order.getShippingFullName(),
                order.getShippingPhone(),
                order.getShippingCity(),
                order.getShippingDistrict(),
                order.getShippingAddressLine(),
                order.getShippingPostalCode()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal
        );
    }
}
