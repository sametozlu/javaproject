package com.ecommerce.api.repository;

import com.ecommerce.api.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAscIdAsc(Long productId);

    boolean existsByProductId(Long productId);

    long countByProductId(Long productId);
}
