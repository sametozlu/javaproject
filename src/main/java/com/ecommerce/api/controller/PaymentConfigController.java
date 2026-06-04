package com.ecommerce.api.controller;

import com.ecommerce.api.dto.payment.PaymentConfigResponse;
import com.ecommerce.api.service.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentConfigController {

    private final StripePaymentService stripePaymentService;

    @GetMapping("/config")
    @Operation(summary = "Payment provider config (Stripe publishable key)")
    public PaymentConfigResponse config() {
        return new PaymentConfigResponse(
                stripePaymentService.isEnabled(),
                stripePaymentService.isEnabled() ? stripePaymentService.getPublishableKey() : null
        );
    }
}
