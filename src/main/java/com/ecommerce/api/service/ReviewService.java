package com.ecommerce.api.service;

import com.ecommerce.api.domain.Product;
import com.ecommerce.api.domain.Review;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.dto.review.ReviewRequest;
import com.ecommerce.api.dto.review.ReviewResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.ReviewRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        productService.findById(productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse addReview(Long productId, ReviewRequest request) {
        Long userId = SecurityUtils.currentUser().getId();
        if (!reviewRepository.hasUserPurchasedProduct(userId, productId)) {
            throw new BadRequestException("You must purchase the product before reviewing");
        }
        if (reviewRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new BadRequestException("You already reviewed this product");
        }

        Product product = productService.getProduct(productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.rating())
                .comment(request.comment())
                .build();
        Review saved = reviewRepository.save(review);
        auditService.log("REVIEW_CREATED", "Product", productId, "rating=" + request.rating());
        return toResponse(saved);
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getProduct().getId(),
                r.getUser().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
