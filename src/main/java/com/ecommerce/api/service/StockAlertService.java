package com.ecommerce.api.service;

import com.ecommerce.api.domain.Product;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.mapper.ProductMapper;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAlertService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        SecurityUtils.requireAdmin();
        return productRepository.findLowStockProducts().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional
    public void checkAndNotify(Product product) {
        if (product.getStockQuantity() <= product.getLowStockThreshold()) {
            emailService.sendLowStockAlert(product.getName(), product.getStockQuantity());
        }
    }
}
