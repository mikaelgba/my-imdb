package auth_user_service.service.impl;

import auth_user_service.dto.*;
import auth_user_service.exception.AccountLockedException;
import auth_user_service.exception.EmailAlreadyExistsException;
import auth_user_service.exception.InvalidCredentialsException;
import auth_user_service.model.SecurityAction;
import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import auth_user_service.repository.UserProfileRepository;
import auth_user_service.repository.UserRepository;
import auth_user_service.service.AuthService;
import auth_user_service.service.RefreshTokenService;
import auth_user_service.service.SecurityAuditService;
import auth_user_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityAuditService securityAuditService;

    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail))
            throw new EmailAlreadyExistsException(normalizedEmail);

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.register(normalizedEmail, encodedPassword, request.role());
        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile(savedUser, request.fullName());
        UserProfile savedProfile = userProfileRepository.save(profile);
        return UserProfileResponse.from(savedProfile);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    securityAuditService.recordEvent(null, null,
                            SecurityAction.LOGIN_FAILED,
                            "E-mail inexistente: " + normalizedEmail);
                    return new InvalidCredentialsException();
                });

        if (user.isAccountLocked()) {
            throw new AccountLockedException();
        }

        if (!user.isActive()) {
            securityAuditService.recordEvent(user.getId(), user.getId(), SecurityAction.LOGIN_FAILED, "Tentativa em conta desativada");
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            securityAuditService.recordFailedLoginAttempt(user.getId(), normalizedEmail);
            throw new InvalidCredentialsException();
        }

        securityAuditService.resetFailedLoginAttempts(user.getId());
        securityAuditService.recordEvent(user.getId(), user.getId(), SecurityAction.LOGIN_SUCCESS, "Login realizado com sucesso");

        String refreshToken = refreshTokenService.createRefreshToken(user);
        return tokenService.generateTokenPair(user, refreshToken);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        User user = refreshTokenService.validateAndRotate(request.refreshToken());
        String newRefreshToken = refreshTokenService.createRefreshToken(user);
        return tokenService.generateTokenPair(user, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.refreshToken());
    }
}
