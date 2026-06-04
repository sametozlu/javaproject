package com.ecommerce.api.controller;

import com.ecommerce.api.dto.payment.PaymentRequest;
import com.ecommerce.api.dto.payment.PaymentResponse;
import com.ecommerce.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/pay")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Process payment (simulated)")
    public PaymentResponse pay(@PathVariable Long orderId, @Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(orderId, request);
    }
}
