package com.ecommerce.api.controller;

import com.ecommerce.api.dto.order.CreateOrderRequest;
import com.ecommerce.api.dto.order.OrderResponse;
import com.ecommerce.api.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a new order")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/api/orders/me")
    @Operation(summary = "List my orders")
    public List<OrderResponse> myOrders() {
        return orderService.getMyOrders();
    }

    @GetMapping("/api/orders/{id}")
    @Operation(summary = "Get order by id")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping("/api/admin/orders")
    @Operation(summary = "List all orders (admin)")
    public List<OrderResponse> allOrders() {
        return orderService.getAllOrders();
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    @Operation(summary = "Update order status (admin)")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateStatus(id, request);
    }

    @PostMapping("/api/orders/{id}/cancel")
    @Operation(summary = "Cancel a pending order (owner or admin)")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}
