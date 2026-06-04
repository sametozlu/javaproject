package com.ecommerce.api.service;

import com.ecommerce.api.domain.Address;
import com.ecommerce.api.domain.*;
import com.ecommerce.api.dto.order.CreateOrderRequest;
import com.ecommerce.api.dto.order.OrderItemRequest;
import com.ecommerce.api.dto.order.OrderResponse;
import com.ecommerce.api.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ForbiddenException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.mapper.OrderMapper;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import com.ecommerce.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final OrderMapper orderMapper;
    private final CouponService couponService;
    private final EmailService emailService;
    private final StockAlertService stockAlertService;
    private final AuditService auditService;
    private final AddressService addressService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressService.getOwnedAddressEntity(request.addressId());

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingCity(address.getCity())
                .shippingDistrict(address.getDistrict())
                .shippingAddressLine(address.getAddressLine())
                .shippingPostalCode(address.getPostalCode())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productService.getProduct(itemRequest.productId());
            productService.reserveStock(product.getId(), itemRequest.quantity());
            product = productService.getProduct(itemRequest.productId());
            stockAlertService.checkAndNotify(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(product.getPrice())
                    .build();

            order.addItem(orderItem);
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        BigDecimal discount = couponService.applyCoupon(request.couponCode(), subtotal);
        order.setDiscountAmount(discount);
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            order.setCouponCode(request.couponCode().trim().toUpperCase());
        }
        order.setTotalAmount(subtotal.subtract(discount));

        Order saved = orderRepository.save(order);
        emailService.sendOrderConfirmation(
                user.getEmail(),
                saved.getId(),
                saved.getTotalAmount().toPlainString(),
                address.getFullName(),
                formatAddress(address)
        );
        auditService.log("ORDER_CREATED", "Order", saved.getId(), "total=" + saved.getTotalAmount());

        return orderMapper.toResponse(orderRepository.findByIdWithItems(saved.getId()).orElse(saved));
    }

    private String formatAddress(Address address) {
        return address.getAddressLine() + ", " + address.getDistrict() + " / " + address.getCity()
                + (address.getPostalCode() != null ? " " + address.getPostalCode() : "");
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        Long userId = SecurityUtils.currentUser().getId();
        return orderRepository.findByUserIdWithItems(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        UserPrincipal principal = SecurityUtils.currentUser();
        if (!order.getUser().getId().equals(principal.getId()) && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You cannot access this order");
        }
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        SecurityUtils.requireAdmin();
        return orderRepository.findAllWithItems().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        SecurityUtils.requireAdmin();
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled order");
        }
        OrderStatus previous = order.getStatus();
        order.setStatus(request.status());
        if (request.status() == OrderStatus.SHIPPED && (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank())) {
            order.setTrackingNumber("SF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        auditService.log("ORDER_STATUS_UPDATED", "Order", id, request.status().name());
        notifyOrderStatusChange(order, previous, request.status());
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        UserPrincipal principal = SecurityUtils.currentUser();
        if (!order.getUser().getId().equals(principal.getId()) && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You cannot cancel this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }

        for (OrderItem item : order.getItems()) {
            productService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        auditService.log("ORDER_CANCELLED", "Order", id, null);
        return orderMapper.toResponse(order);
    }

    private void notifyOrderStatusChange(Order order, OrderStatus previous, OrderStatus next) {
        if (previous == next) {
            return;
        }
        String email = order.getUser().getEmail();
        if (next == OrderStatus.SHIPPED && previous != OrderStatus.SHIPPED) {
            emailService.sendOrderShipped(email, order.getId(), order.getTrackingNumber());
        }
        if (next == OrderStatus.DELIVERED && previous != OrderStatus.DELIVERED) {
            emailService.sendOrderDelivered(email, order.getId());
        }
    }
}
