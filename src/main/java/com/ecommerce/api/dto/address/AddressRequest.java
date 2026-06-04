package com.ecommerce.api.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 50) String title,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 80) String district,
        @NotBlank @Size(max = 300) String addressLine,
        @Size(max = 10) String postalCode,
        boolean defaultAddress
) {
}
