package com.ecommerce.api.repository;

import com.ecommerce.api.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
}
