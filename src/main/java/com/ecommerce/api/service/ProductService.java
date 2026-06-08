package com.ecommerce.api.service;

import com.ecommerce.api.domain.Category;
import com.ecommerce.api.domain.Product;
import com.ecommerce.api.domain.ProductImage;
import com.ecommerce.api.dto.common.PageResponse;
import com.ecommerce.api.dto.product.ProductRequest;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ConflictException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.mapper.ProductMapper;
import com.ecommerce.api.repository.ProductImageRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.repository.ProductSpecifications;
import com.ecommerce.api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            String query,
            Long categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            Boolean inStock,
            int page,
            int size,
            String sort
    ) {
        if (isPopularSort(sort) && !hasFilters(query, categoryId, minPrice, maxPrice, inStock)) {
            return searchPopular(page, size);
        }

        Sort sorting = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Product> result = productRepository.findAll(
                ProductSpecifications.withFilters(query, categoryId, minPrice, maxPrice, null, inStock),
                pageable
        );
        return PageResponse.from(result.map(productMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFeatured(int size) {
        int limit = Math.min(Math.max(size, 1), 24);
        return productRepository.findByFeaturedTrue(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getBestsellers(int size) {
        int limit = Math.min(Math.max(size, 1), 24);
        List<Long> topIds = reviewRepository.findTopProductIdsByReviewCount(PageRequest.of(0, limit));
        List<ProductResponse> result = new ArrayList<>();
        for (Long id : topIds) {
            productRepository.findById(id).ifPresent(p -> result.add(productMapper.toResponse(p)));
        }
        if (result.size() < limit) {
            for (Product p : productRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()) {
                if (result.size() >= limit) break;
                if (result.stream().noneMatch(r -> r.id().equals(p.getId()))) {
                    result.add(productMapper.toResponse(p));
                }
            }
        }
        return result.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getProduct(id));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Category category = categoryService.getById(request.categoryId());
        Product product = productMapper.toEntity(request, category);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        Category category = categoryService.getById(request.categoryId());
        productMapper.updateEntity(product, request, category);
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void reserveStock(Long productId, int quantity) {
        Product product = getProduct(productId);
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        try {
            productRepository.saveAndFlush(product);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Stock updated by another order. Please try again.");
        }
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void restoreStock(Long productId, int quantity) {
        Product product = getProduct(productId);
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateImage(Long id, String imageUrl) {
        Product product = getProduct(id);
        product.setImageUrl(imageUrl);
        addGalleryImage(product, imageUrl, true);
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse addGalleryImage(Long id, String imageUrl) {
        Product product = getProduct(id);
        addGalleryImage(product, imageUrl, false);
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            product.setImageUrl(imageUrl);
        }
        return productMapper.toResponse(product);
    }

    private void addGalleryImage(Product product, String imageUrl, boolean primary) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        boolean exists = productImageRepository.findByProductIdOrderByDisplayOrderAscIdAsc(product.getId())
                .stream()
                .anyMatch(img -> imageUrl.equals(img.getImageUrl()));
        if (exists) {
            return;
        }
        int order = primary ? 0 : (int) productImageRepository.countByProductId(product.getId());
        productImageRepository.save(ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .displayOrder(order)
                .build());
    }

    private PageResponse<ProductResponse> searchPopular(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Long> ids = reviewRepository.findTopProductIdsByReviewCount(pageable);
        List<ProductResponse> content = new ArrayList<>();
        for (Long id : ids) {
            productRepository.findById(id).ifPresent(p -> content.add(productMapper.toResponse(p)));
        }
        long total = productRepository.count();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages, page == 0, page >= totalPages - 1);
    }

    private boolean isPopularSort(String sort) {
        return sort != null && (sort.equalsIgnoreCase("popular") || sort.equalsIgnoreCase("reviewCount,desc"));
    }

    private boolean hasFilters(String query, Long categoryId, java.math.BigDecimal minPrice,
                               java.math.BigDecimal maxPrice, Boolean inStock) {
        return (query != null && !query.isBlank())
                || categoryId != null
                || minPrice != null
                || maxPrice != null
                || Boolean.TRUE.equals(inStock);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank() || isPopularSort(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
