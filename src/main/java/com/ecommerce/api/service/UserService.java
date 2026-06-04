package com.ecommerce.api.service;

import com.ecommerce.api.dto.user.UserProfileResponse;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        Long userId = SecurityUtils.currentUser().getId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long orderCount = orderRepository.countByUserId(userId);
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
