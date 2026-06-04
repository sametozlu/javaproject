package com.ecommerce.api.service;

import com.ecommerce.api.domain.PasswordResetToken;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.repository.PasswordResetTokenRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.app-url:http://localhost:8080}")
    private String appUrl;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String raw = UUID.randomUUID().toString();
            resetTokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.sha256(raw))
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build());
            String link = appUrl + "/?reset=" + raw;
            emailService.sendPasswordReset(user.getEmail(), user.getFullName(), link);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokenRepository.findByTokenHashAndUsedFalse(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token expired");
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        token.setUsed(true);
        userRepository.save(user);
    }
}
