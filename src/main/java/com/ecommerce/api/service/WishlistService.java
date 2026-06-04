package com.ecommerce.api.service;

import com.ecommerce.api.domain.Product;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.domain.Wishlist;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.mapper.ProductMapper;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.repository.WishlistRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlist() {
        Long userId = SecurityUtils.currentUser().getId();
        return wishlistRepository.findByUserIdWithProduct(userId).stream()
                .map(w -> productMapper.toResponse(w.getProduct()))
                .toList();
    }

    @Transactional
    public void add(Long productId) {
        Long userId = SecurityUtils.currentUser().getId();
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("Already in wishlist");
        }
        Product product = productService.getProduct(productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        wishlistRepository.save(Wishlist.builder().user(user).product(product).build());
    }

    @Transactional
    public void remove(Long productId) {
        Long userId = SecurityUtils.currentUser().getId();
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlistRepository::delete);
    }
}
