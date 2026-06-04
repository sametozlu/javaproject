package com.ecommerce.api.controller;

import com.ecommerce.api.dto.review.ReviewRequest;
import com.ecommerce.api.dto.review.ReviewResponse;
import com.ecommerce.api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "List product reviews (public)")
    public List<ReviewResponse> list(@PathVariable Long productId) {
        return reviewService.getProductReviews(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add review (must have purchased)")
    public ReviewResponse create(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.addReview(productId, request);
    }
}
