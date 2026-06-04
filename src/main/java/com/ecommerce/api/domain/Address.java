package com.ecommerce.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String district;

    @Column(name = "address_line", nullable = false, length = 300)
    private String addressLine;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultAddress = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
