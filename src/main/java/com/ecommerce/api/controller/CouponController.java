package com.ecommerce.api.controller;

import com.ecommerce.api.dto.coupon.CouponRequest;
import com.ecommerce.api.dto.coupon.CouponValidationResponse;
import com.ecommerce.api.dto.coupon.ValidateCouponRequest;
import com.ecommerce.api.domain.Coupon;
import com.ecommerce.api.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Coupons")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/api/coupons/validate")
    @Operation(summary = "Validate coupon code (public)")
    public CouponValidationResponse validate(@Valid @RequestBody ValidateCouponRequest request) {
        return couponService.validate(request.code(), request.orderAmount());
    }

    @GetMapping("/api/admin/coupons")
    @Operation(summary = "List coupons (admin)")
    public List<Coupon> list() {
        return couponService.listAll();
    }

    @PostMapping("/api/admin/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create coupon (admin)")
    public Coupon create(@Valid @RequestBody CouponRequest request) {
        Coupon coupon = Coupon.builder()
                .code(request.code())
                .discountPercent(request.discountPercent())
                .minOrderAmount(request.minOrderAmount())
                .maxUses(request.maxUses())
                .expiresAt(request.expiresAt())
                .build();
        return couponService.create(coupon);
    }
}
