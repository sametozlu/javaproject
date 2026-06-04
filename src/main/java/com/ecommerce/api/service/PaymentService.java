package com.ecommerce.api.service;

import com.ecommerce.api.domain.*;
import com.ecommerce.api.dto.payment.PaymentRequest;
import com.ecommerce.api.dto.payment.PaymentResponse;
import com.ecommerce.api.dto.payment.StripePaymentIntentResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ForbiddenException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.PaymentRepository;
import com.ecommerce.api.security.SecurityUtils;
import com.ecommerce.api.security.UserPrincipal;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final StripePaymentService stripePaymentService;

    @Transactional
    public PaymentResponse processPayment(Long orderId, PaymentRequest request) {
        return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::toResponse)
                .orElseGet(() -> processNewPayment(orderId, request));
    }

    @Transactional(readOnly = true)
    public StripePaymentIntentResponse createStripePaymentIntent(Long orderId) {
        Order order = loadOrderForPayment(orderId);
        return stripePaymentService.createPaymentIntent(order);
    }

    private PaymentResponse processNewPayment(Long orderId, PaymentRequest request) {
        Order order = loadOrderForPayment(orderId);

        var existingPaid = paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getStatus() == PaymentStatus.PAID);
        if (existingPaid.isPresent()) {
            return toResponse(existingPaid.get());
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not awaiting payment");
        }

        if (StringUtils.hasText(request.paymentIntentId())) {
            return completeStripePayment(order, orderId, request);
        }

        boolean fail = Boolean.TRUE.equals(request.simulateFailure());
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .idempotencyKey(request.idempotencyKey())
                .status(fail ? PaymentStatus.FAILED : PaymentStatus.PAID)
                .build());

        if (fail) {
            auditService.log("PAYMENT_FAILED", "Payment", payment.getId(), "order=" + orderId + ",mode=simulate");
            return new PaymentResponse(
                    payment.getId(),
                    orderId,
                    PaymentStatus.FAILED,
                    payment.getAmount(),
                    "Payment declined (simulated)"
            );
        }

        return markOrderPaid(order, payment, orderId, "Payment successful (simulated)");
    }

    private PaymentResponse completeStripePayment(Order order, Long orderId, PaymentRequest request) {
        if (!stripePaymentService.isEnabled()) {
            throw new BadRequestException("Stripe is not configured");
        }
        PaymentIntent intent = stripePaymentService.retrieveSucceededIntent(request.paymentIntentId(), orderId);
        var existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .idempotencyKey(request.idempotencyKey())
                .stripePaymentIntentId(intent.getId())
                .status(PaymentStatus.PAID)
                .build());

        return markOrderPaid(order, payment, orderId, "Payment successful (Stripe)");
    }

    private PaymentResponse markOrderPaid(Order order, Payment payment, Long orderId, String message) {
        order.setStatus(OrderStatus.CONFIRMED);
        auditService.log("PAYMENT_SUCCEEDED", "Payment", payment.getId(),
                "order=" + orderId + (payment.getStripePaymentIntentId() != null ? ",stripe=true" : ",simulate=true"));
        emailService.sendPaymentSuccess(order.getUser().getEmail(), orderId);
        return new PaymentResponse(
                payment.getId(),
                orderId,
                PaymentStatus.PAID,
                payment.getAmount(),
                message
        );
    }

    private Order loadOrderForPayment(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        UserPrincipal principal = SecurityUtils.currentUser();
        if (!order.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("You cannot pay for this order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order");
        }
        return order;
    }

    private PaymentResponse toResponse(Payment payment) {
        String message = switch (payment.getStatus()) {
            case PAID -> payment.getStripePaymentIntentId() != null
                    ? "Payment successful (Stripe)"
                    : "Payment successful";
            case FAILED -> "Payment declined";
            default -> "Payment pending";
        };
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getStatus(),
                payment.getAmount(),
                message
        );
    }
}
