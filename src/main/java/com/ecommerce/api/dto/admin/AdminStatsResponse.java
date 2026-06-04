package com.ecommerce.api.dto.admin;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        long pendingOrders,
        BigDecimal totalRevenue
) {
}
