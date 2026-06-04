package com.ecommerce.api.repository;

import com.ecommerce.api.domain.Order;
import com.ecommerce.api.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByUserId(Long userId);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> com.ecommerce.api.domain.OrderStatus.CANCELLED")
    BigDecimal sumActiveRevenue();

    List<Order> findByCreatedAtAfter(Instant since);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product ORDER BY o.createdAt DESC")
    List<Order> findAllWithItems();
}
