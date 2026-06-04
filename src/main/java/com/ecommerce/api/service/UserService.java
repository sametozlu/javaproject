package com.ecommerce.api.service;

import com.ecommerce.api.domain.User;
import com.ecommerce.api.dto.user.UpdateProfileRequest;
import com.ecommerce.api.dto.user.UserProfileResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        Long userId = SecurityUtils.currentUser().getId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long orderCount = orderRepository.countByUserId(userId);
        return toProfile(user, orderCount);
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtils.currentUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (StringUtils.hasText(request.fullName())) {
            user.setFullName(request.fullName().trim());
        }

        if (StringUtils.hasText(request.newPassword())) {
            if (!StringUtils.hasText(request.currentPassword())) {
                throw new BadRequestException("Current password is required to set a new password");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        long orderCount = orderRepository.countByUserId(userId);
        return toProfile(user, orderCount);
    }

    private UserProfileResponse toProfile(User user, long orderCount) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                orderCount
        );
    }
}
