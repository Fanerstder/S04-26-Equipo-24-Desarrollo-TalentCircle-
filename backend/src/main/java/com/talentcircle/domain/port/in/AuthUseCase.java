package com.talentcircle.domain.port.in;

import java.util.Optional;

public interface AuthUseCase {
    record LoginRequest(String email, String password) {}
    record LoginResponse(String accessToken, String refreshToken, String expiresIn, UserDto user) {}
    record RefreshRequest(String refreshToken) {}
    record UserDto(String id, String email, String fullName, String role) {}

    LoginResponse login(LoginRequest request);
    LoginResponse refresh(RefreshRequest request);
    void logout(String userId);
    UserDto createUser(String email, String password, String fullName, String role);
    UserDto updateUser(String userId, String fullName, String role, Boolean active);
    void changePassword(String userId, String currentPassword, String newPassword);
}
