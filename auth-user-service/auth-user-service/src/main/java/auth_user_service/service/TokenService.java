package auth_user_service.service;

import auth_user_service.dto.TokenResponse;
import auth_user_service.model.User;

public interface TokenService {
    TokenResponse generateTokenPair(User user, String refreshToken);
}