package com.ecommerce.api.service;

import com.ecommerce.api.domain.RefreshToken;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.repository.RefreshTokenRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.JwtProperties;
import com.ecommerce.api.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String issueForUser(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
        String raw = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(raw))
                .expiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()))
                .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    @Transactional(readOnly = true)
    public User validateAndGetUser(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired");
        }
        return stored.getUser();
    }

    @Transactional
    public void revokeForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
