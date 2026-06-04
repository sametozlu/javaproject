package com.ecommerce.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 50)
    private String couponCode;

    @Column(length = 120)
    private String shippingFullName;

    @Column(length = 20)
    private String shippingPhone;

    @Column(length = 80)
    private String shippingCity;

    @Column(length = 80)
    private String shippingDistrict;

    @Column(length = 300)
    private String shippingAddressLine;

    @Column(length = 10)
    private String shippingPostalCode;

    @Column(length = 50)
    private String trackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
