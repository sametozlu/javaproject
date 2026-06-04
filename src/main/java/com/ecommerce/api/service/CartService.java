package com.ecommerce.api.service;

import com.ecommerce.api.domain.*;
import com.ecommerce.api.dto.cart.*;
import com.ecommerce.api.dto.order.CreateOrderRequest;
import com.ecommerce.api.dto.order.OrderItemRequest;
import com.ecommerce.api.dto.order.OrderResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.CartRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Cart cart = getOrCreateCart();
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        Cart cart = getOrCreateCart();
        Product product = productService.getProduct(request.productId());

        if (product.getStockQuantity() < request.quantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
        } else {
            cart.addItem(CartItem.builder().product(product).quantity(request.quantity()).build());
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long productId, int quantity) {
        Cart cart = getOrCreateCart();
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (item.getProduct().getStockQuantity() < quantity) {
                throw new BadRequestException("Insufficient stock");
            }
            item.setQuantity(quantity);
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Cart cart = getOrCreateCart();
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toResponse(cart);
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Cart cart = getOrCreateCart();
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        var items = cart.getItems().stream()
                .map(i -> new OrderItemRequest(i.getProduct().getId(), i.getQuantity()))
                .toList();

        OrderResponse order = orderService.createOrder(
                new CreateOrderRequest(items, request.couponCode(), request.addressId()));
        cart.getItems().clear();
        return order;
    }

    private Cart getOrCreateCart() {
        Long userId = SecurityUtils.currentUser().getId();
        return cartRepository.findByUserIdWithItems(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return cartRepository.save(Cart.builder().user(user).build());
        });
    }

    private CartResponse toResponse(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        var items = cart.getItems().stream().map(i -> {
            BigDecimal line = i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CartItemResponse(
                    i.getProduct().getId(),
                    i.getProduct().getName(),
                    i.getProduct().getPrice(),
                    i.getQuantity(),
                    line,
                    i.getProduct().getImageUrl()
            );
        }).toList();
        for (CartItemResponse item : items) {
            subtotal = subtotal.add(item.subtotal());
        }
        return new CartResponse(cart.getId(), items, subtotal);
    }
}
