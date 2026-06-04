package com.ecommerce.api.controller;

import com.ecommerce.api.dto.user.UpdateProfileRequest;
import com.ecommerce.api.dto.user.UserProfileResponse;
import com.ecommerce.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserProfileResponse me() {
        return userService.getCurrentProfile();
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateCurrentProfile(request);
    }
}
