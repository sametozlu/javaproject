package com.ecommerce.api.service;

import com.ecommerce.api.domain.Coupon;
import com.ecommerce.api.dto.coupon.CouponValidationResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.CouponRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CouponValidationResponse validate(String code, BigDecimal orderAmount) {
        Coupon coupon = getValidCoupon(code, orderAmount);
        BigDecimal discount = calculateDiscount(orderAmount, coupon.getDiscountPercent());
        return new CouponValidationResponse(true, coupon.getCode(), coupon.getDiscountPercent(), discount, "Coupon applied");
    }

    @Transactional
    public BigDecimal applyCoupon(String code, BigDecimal orderAmount) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        Coupon coupon = getValidCoupon(code, orderAmount);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        BigDecimal discount = calculateDiscount(orderAmount, coupon.getDiscountPercent());
        auditService.log("COUPON_APPLIED", "Coupon", coupon.getId(), code + " -" + discount);
        return discount;
    }

    @Transactional(readOnly = true)
    public List<Coupon> listAll() {
        SecurityUtils.requireAdmin();
        return couponRepository.findAll();
    }

    @Transactional
    public Coupon create(Coupon coupon) {
        SecurityUtils.requireAdmin();
        coupon.setCode(coupon.getCode().toUpperCase());
        Coupon saved = couponRepository.save(coupon);
        auditService.log("COUPON_CREATED", "Coupon", saved.getId(), saved.getCode());
        return saved;
    }

    private Coupon getValidCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new BadRequestException("Coupon is inactive");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Coupon expired");
        }
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("Coupon usage limit reached");
        }
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Minimum order amount: " + coupon.getMinOrderAmount());
        }
        return coupon;
    }

    private BigDecimal calculateDiscount(BigDecimal amount, int percent) {
        return amount.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
