package com.ecommerce.api.repository;

import com.ecommerce.api.domain.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.id = :productId")
    double averageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);

    @Query("SELECT r.product.id FROM Review r GROUP BY r.product.id ORDER BY COUNT(r) DESC")
    List<Long> findTopProductIdsByReviewCount(Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.id = :productId AND o.status <> com.ecommerce.api.domain.OrderStatus.CANCELLED")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
