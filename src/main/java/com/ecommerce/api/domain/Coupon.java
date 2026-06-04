package com.ecommerce.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer discountPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    private Integer maxUses;

    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private Instant expiresAt;
}
