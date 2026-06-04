package com.ecommerce.api.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        String imageUrl
) {
}
