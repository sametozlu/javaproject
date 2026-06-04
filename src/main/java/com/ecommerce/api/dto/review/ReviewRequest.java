package com.ecommerce.api.dto.review;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String comment
) {
}
