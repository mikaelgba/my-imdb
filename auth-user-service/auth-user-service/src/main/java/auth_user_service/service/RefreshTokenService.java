package auth_user_service.service;

import auth_user_service.model.User;

public interface RefreshTokenService {
    String createRefreshToken(User user);
    User validateAndRotate(String rawToken);
    void revokeToken(String rawToken);
    void revokeAllUserTokens(User user);
}