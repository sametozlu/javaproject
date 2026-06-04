package com.ecommerce.api.service;

import com.ecommerce.api.domain.*;
import com.ecommerce.api.dto.payment.PaymentRequest;
import com.ecommerce.api.dto.payment.PaymentResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ForbiddenException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.PaymentRepository;
import com.ecommerce.api.security.SecurityUtils;
import com.ecommerce.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public PaymentResponse processPayment(Long orderId, PaymentRequest request) {
        return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::toResponse)
                .orElseGet(() -> processNewPayment(orderId, request));
    }

    private PaymentResponse processNewPayment(Long orderId, PaymentRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        UserPrincipal principal = SecurityUtils.currentUser();
        if (!order.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("You cannot pay for this order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order");
        }

        var existingPaid = paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getStatus() == PaymentStatus.PAID);
        if (existingPaid.isPresent()) {
            return toResponse(existingPaid.get());
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not awaiting payment");
        }

        boolean fail = Boolean.TRUE.equals(request.simulateFailure());
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .idempotencyKey(request.idempotencyKey())
                .status(fail ? PaymentStatus.FAILED : PaymentStatus.PAID)
                .build());

        if (fail) {
            auditService.log("PAYMENT_FAILED", "Payment", payment.getId(), "order=" + orderId);
            return new PaymentResponse(
                    payment.getId(),
                    orderId,
                    PaymentStatus.FAILED,
                    payment.getAmount(),
                    "Payment declined (simulated)"
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);
        auditService.log("PAYMENT_SUCCEEDED", "Payment", payment.getId(), "order=" + orderId);
        emailService.sendPaymentSuccess(order.getUser().getEmail(), orderId);
        return new PaymentResponse(
                payment.getId(),
                orderId,
                PaymentStatus.PAID,
                payment.getAmount(),
                "Payment successful"
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        String message = switch (payment.getStatus()) {
            case PAID -> "Payment successful";
            case FAILED -> "Payment declined (simulated)";
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
