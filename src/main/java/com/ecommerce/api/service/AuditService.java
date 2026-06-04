package com.ecommerce.api.service;

import com.ecommerce.api.domain.AuditLog;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.dto.admin.AuditLogResponse;
import com.ecommerce.api.repository.AuditLogRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(String action, String entityType, Long entityId, String details) {
        User user = null;
        try {
            Long userId = SecurityUtils.currentUser().getId();
            user = userRepository.getReferenceById(userId);
        } catch (Exception ignored) {
        }
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> recentLogs() {
        SecurityUtils.requireAdmin();
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        String email = log.getUser() != null ? log.getUser().getEmail() : "system";
        return new AuditLogResponse(
                log.getId(),
                email,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
