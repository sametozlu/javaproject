package com.ecommerce.api.controller;

import com.ecommerce.api.dto.cart.*;
import com.ecommerce.api.dto.order.OrderResponse;
import com.ecommerce.api.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get my cart")
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(request);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity")
    public CartResponse updateItem(@PathVariable Long productId, @RequestParam int quantity) {
        return cartService.updateItem(productId, quantity);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public CartResponse removeItem(@PathVariable Long productId) {
        return cartService.removeItem(productId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Checkout cart and create order")
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return cartService.checkout(request);
    }
}
