package com.ecommerce.api.service;

import com.ecommerce.api.domain.Order;
import com.ecommerce.api.dto.payment.StripePaymentIntentResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class StripePaymentService {

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @Value("${app.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${app.stripe.currency:try}")
    private String currency;

    @PostConstruct
    void init() {
        if (isEnabled()) {
            Stripe.apiKey = secretKey;
        }
    }

    public boolean isEnabled() {
        return StringUtils.hasText(secretKey) && StringUtils.hasText(publishableKey);
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public StripePaymentIntentResponse createPaymentIntent(Order order) {
        if (!isEnabled()) {
            throw new BadRequestException("Stripe is not configured");
        }
        long amountMinor = toMinorUnits(order.getTotalAmount());
        try {
            PaymentIntent intent = PaymentIntent.create(PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(currency)
                    .putMetadata("orderId", order.getId().toString())
                    .putMetadata("userId", order.getUser().getId().toString())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .build());
            return new StripePaymentIntentResponse(
                    intent.getClientSecret(),
                    intent.getId(),
                    order.getTotalAmount(),
                    currency
            );
        } catch (StripeException e) {
            throw new BadRequestException("Stripe error: " + e.getMessage());
        }
    }

    public PaymentIntent retrieveSucceededIntent(String paymentIntentId, Long orderId) {
        if (!isEnabled()) {
            throw new BadRequestException("Stripe is not configured");
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            String metaOrderId = intent.getMetadata() != null ? intent.getMetadata().get("orderId") : null;
            if (metaOrderId == null || !metaOrderId.equals(orderId.toString())) {
                throw new BadRequestException("Payment intent does not match order");
            }
            if (!"succeeded".equals(intent.getStatus())) {
                throw new BadRequestException("Payment not completed: " + intent.getStatus());
            }
            return intent;
        } catch (StripeException e) {
            throw new BadRequestException("Stripe error: " + e.getMessage());
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
