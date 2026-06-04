package com.ecommerce.api.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public record ChartDataResponse(
        List<String> labels,
        List<Long> orderCounts,
        List<BigDecimal> revenues
) {
}
