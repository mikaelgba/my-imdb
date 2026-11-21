package auth_user_service.service;

import auth_user_service.dto.*;

public interface AuthService {
    UserProfileResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}
