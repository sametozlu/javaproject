package com.ecommerce.api.repository;

import com.ecommerce.api.domain.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Optional<StockAlert> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
