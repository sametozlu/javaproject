package com.ecommerce.api.repository;

import com.ecommerce.api.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByFeaturedTrue(Pageable pageable);

    Optional<Product> findByName(String name);

    List<Product> findByStockQuantityLessThanEqual(Integer threshold);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();
}
