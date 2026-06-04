package com.ecommerce.api.dto.admin;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String userEmail,
        String action,
        String entityType,
        Long entityId,
        String details,
        Instant createdAt
) {
}
