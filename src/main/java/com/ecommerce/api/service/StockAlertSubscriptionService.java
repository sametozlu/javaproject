package com.ecommerce.api.service;

import com.ecommerce.api.domain.Product;
import com.ecommerce.api.domain.StockAlert;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.repository.StockAlertRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockAlertSubscriptionService {

    private final StockAlertRepository stockAlertRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public void subscribe(Long productId) {
        var user = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (product.getStockQuantity() > 0) {
            throw new BadRequestException("Product is already in stock");
        }
        if (stockAlertRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BadRequestException("You are already subscribed for this product");
        }
        stockAlertRepository.save(StockAlert.builder()
                .user(user)
                .product(product)
                .build());
    }
}
