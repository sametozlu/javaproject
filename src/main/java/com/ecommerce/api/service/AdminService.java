package com.ecommerce.api.service;

import com.ecommerce.api.domain.Order;
import com.ecommerce.api.domain.OrderStatus;
import com.ecommerce.api.dto.admin.AdminStatsResponse;
import com.ecommerce.api.dto.admin.ChartDataResponse;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        SecurityUtils.requireAdmin();
        return new AdminStatsResponse(
                userRepository.count(),
                productRepository.count(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.sumActiveRevenue()
        );
    }

    @Transactional(readOnly = true)
    public ChartDataResponse getOrderCharts(int months) {
        SecurityUtils.requireAdmin();
        Instant since = Instant.now().minus(months * 30L, ChronoUnit.DAYS);
        List<Order> orders = orderRepository.findByCreatedAtAfter(since);

        Map<YearMonth, long[]> buckets = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            buckets.put(ym, new long[]{0, 0}); // count, revenue cents placeholder
        }

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            YearMonth ym = YearMonth.from(order.getCreatedAt().atZone(ZoneId.systemDefault()));
            if (buckets.containsKey(ym)) {
                buckets.get(ym)[0]++;
            }
        }

        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();

        for (Map.Entry<YearMonth, long[]> e : buckets.entrySet()) {
            labels.add(e.getKey().toString());
            counts.add(e.getValue()[0]);
            BigDecimal monthRevenue = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .filter(o -> YearMonth.from(o.getCreatedAt().atZone(ZoneId.systemDefault())).equals(e.getKey()))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenues.add(monthRevenue);
        }

        return new ChartDataResponse(labels, counts, revenues);
    }
}
