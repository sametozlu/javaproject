package com.ecommerce.api.mapper;

import com.ecommerce.api.domain.Category;
import com.ecommerce.api.domain.Product;
import com.ecommerce.api.dto.product.ProductRequest;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.repository.ProductImageRepository;
import com.ecommerce.api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;

    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .category(category)
                .lowStockThreshold(request.lowStockThreshold() != null ? request.lowStockThreshold() : 5)
                .build();
    }

    public void updateEntity(Product product, ProductRequest request, Category category) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);
        if (request.lowStockThreshold() != null) {
            product.setLowStockThreshold(request.lowStockThreshold());
        }
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        double avg = reviewRepository.averageRatingByProductId(product.getId());
        long reviewCount = reviewRepository.countByProductId(product.getId());
        List<String> imageUrls = productImageRepository.findByProductIdOrderByDisplayOrderAscIdAsc(product.getId())
                .stream()
                .map(img -> img.getImageUrl())
                .toList();
        if (imageUrls.isEmpty() && product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            imageUrls = List.of(product.getImageUrl());
        }
        String primary = product.getImageUrl() != null && !product.getImageUrl().isBlank()
                ? product.getImageUrl()
                : imageUrls.isEmpty() ? null : imageUrls.get(0);

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                category.getId(),
                category.getName(),
                category.getSlug(),
                primary,
                imageUrls,
                Math.round(avg * 10.0) / 10.0,
                reviewCount,
                product.getCreatedAt()
        );
    }
}
