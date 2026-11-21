package auth_user_service.service.impl;

import auth_user_service.config.RsaKeyProperties;
import auth_user_service.exception.InvalidTokenException;
import auth_user_service.model.RefreshToken;
import auth_user_service.model.User;
import auth_user_service.repository.RefreshTokenRepository;
import auth_user_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RsaKeyProperties rsaKeys;

    @Override
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateOpaqueToken();
        String tokenHash = hashSha256(rawToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(rsaKeys.refreshExpirationSeconds());

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Override
    @Transactional
    public User validateAndRotate(String rawToken) {
        String tokenHash = hashSha256(rawToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashWithUser(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token não encontrado."));

        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeAllActiveTokensByUserId(storedToken.getUser().getId());
            throw new InvalidTokenException("Refresh token já utilizado. Todas as sessões foram revogadas por segurança.");
        }

        if (!storedToken.isValid()) {
            storedToken.revoke();
            refreshTokenRepository.saveAndFlush(storedToken);
            throw new InvalidTokenException("Refresh token expirado ou usuário desativado.");
        }

        storedToken.revoke();
        refreshTokenRepository.saveAndFlush(storedToken);
        return storedToken.getUser();
    }

    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashSha256(rawToken);
        refreshTokenRepository.findByTokenHashWithUser(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.saveAndFlush(token);
                });
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllActiveTokensByUserId(user.getId());
    }

    private String hashSha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }
}
