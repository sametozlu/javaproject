package com.ecommerce.api.service;

import com.ecommerce.api.domain.Role;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.dto.auth.AuthResponse;
import com.ecommerce.api.dto.auth.LoginRequest;
import com.ecommerce.api.dto.auth.RegisterRequest;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("User not found"));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokenService.validateAndGetUser(rawRefreshToken);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        String refresh = refreshTokenService.issueForUser(user);
        return AuthResponse.of(token, refresh, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
