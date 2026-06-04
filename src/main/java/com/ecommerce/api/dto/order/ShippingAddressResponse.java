package com.ecommerce.api.dto.order;

public record ShippingAddressResponse(
        String fullName,
        String phone,
        String city,
        String district,
        String addressLine,
        String postalCode
) {
}
