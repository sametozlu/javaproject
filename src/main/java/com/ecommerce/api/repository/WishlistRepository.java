package com.ecommerce.api.repository;

import com.ecommerce.api.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserIdWithProduct(@Param("userId") Long userId);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
