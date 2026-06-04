package com.ecommerce.api.mapper;

import com.ecommerce.api.domain.Category;
import com.ecommerce.api.domain.Product;
import com.ecommerce.api.dto.product.ProductRequest;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ReviewRepository reviewRepository;

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
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                category.getId(),
                category.getName(),
                category.getSlug(),
                product.getImageUrl(),
                Math.round(avg * 10.0) / 10.0,
                reviewCount,
                product.getCreatedAt()
        );
    }
}
