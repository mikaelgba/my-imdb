package auth_user_service.auth.service;

import auth_user_service.config.RsaKeyProperties;
import auth_user_service.exception.InvalidTokenException;
import auth_user_service.model.RefreshToken;
import auth_user_service.model.Role;
import auth_user_service.model.User;
import auth_user_service.repository.RefreshTokenRepository;
import auth_user_service.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RsaKeyProperties rsaKeys;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.register("user@myimdb.com", "encodedPass", Role.USER);
    }

    @Test
    @DisplayName("Deve gerar token opaco, salvar hash no repositório e retornar token puro")
    void shouldCreateAndPersistHashedRefreshToken() {
        when(rsaKeys.refreshExpirationSeconds()).thenReturn(604800L);

        String rawToken = refreshTokenService.createRefreshToken(activeUser);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve rotacionar token válido revogando o anterior e retornando o usuário")
    void shouldRotateValidToken() {
        RefreshToken stored = new RefreshToken(activeUser, "someHash", OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(stored));

        User result = refreshTokenService.validateAndRotate("raw-token");

        assertEquals(activeUser, result);
        assertTrue(stored.isRevoked());
    }

    @Test
    @DisplayName("Deve revogar todas as sessões do usuário ao detectar reuso de token já revogado")
    void shouldRevokeAllSessionsOnTokenReuseDetection() {
        RefreshToken alreadyRevoked = new RefreshToken(activeUser, "someHash", OffsetDateTime.now().plusDays(1));
        alreadyRevoked.revoke();

        when(refreshTokenRepository.findByTokenHashWithUser(any()))
                .thenReturn(Optional.of(alreadyRevoked));

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.validateAndRotate("reused-token")
        );
        verify(refreshTokenRepository).revokeAllActiveTokensByUserId(activeUser.getId());
    }

    @Test
    @DisplayName("Deve lançar exceção quando refresh token não existir")
    void shouldThrowWhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                refreshTokenService.validateAndRotate("unknown-token")
        );
    }
}