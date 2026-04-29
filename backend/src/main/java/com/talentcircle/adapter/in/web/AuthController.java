package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.AuthUseCase;
import com.talentcircle.domain.port.in.AuthUseCase.LoginRequest;
import com.talentcircle.domain.port.in.AuthUseCase.LoginResponse;
import com.talentcircle.domain.port.in.AuthUseCase.RefreshRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController`
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        LoginResponse response = authUseCase.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Extract user ID from token and logout
            // authUseCase.logout(userId);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestParam String currentPassword,
                                             @RequestParam String newPassword) {
        // Get user ID from security context
        // authUseCase.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.noContent().build();
    }
}
