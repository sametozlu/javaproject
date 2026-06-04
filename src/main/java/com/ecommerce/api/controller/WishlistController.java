package com.ecommerce.api.controller;

import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public List<ProductResponse> getWishlist() {
        return wishlistService.getWishlist();
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add product to wishlist")
    public void add(@PathVariable Long productId) {
        wishlistService.add(productId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove product from wishlist")
    public void remove(@PathVariable Long productId) {
        wishlistService.remove(productId);
    }
}
