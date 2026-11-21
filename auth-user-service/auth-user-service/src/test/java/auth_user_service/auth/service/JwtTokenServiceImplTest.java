package auth_user_service.auth.service;

import auth_user_service.config.RsaKeyProperties;
import auth_user_service.dto.TokenResponse;
import auth_user_service.model.Role;
import auth_user_service.model.User;
import auth_user_service.service.impl.JwtTokenServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceImplTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private RsaKeyProperties rsaKeys;

    @InjectMocks
    private JwtTokenServiceImpl tokenService;

    @Test
    @DisplayName("Deve emitir JWT contendo claims subject, email, scope e expiração corretos")
    void shouldGenerateJwtWithExpectedClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.register("editor@myimdb.com", "hash", Role.EDITOR);
        ReflectionTestUtils.setField(user, "id", userId);

        when(rsaKeys.expirationSeconds()).thenReturn(900L);
        Jwt mockJwt = new Jwt("encoded.jwt.token", Instant.now(), Instant.now().plusSeconds(900),
                java.util.Map.of("alg", "RS256"), java.util.Map.of("sub", userId.toString()));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        TokenResponse response = tokenService.generateTokenPair(user, "opaque-refresh");

        assertEquals("encoded.jwt.token", response.accessToken());
        assertEquals("opaque-refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        var claims = captor.getValue().getClaims();
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("editor@myimdb.com", claims.getClaimAsString("email"));
        assertEquals("EDITOR", claims.getClaimAsString("scope"));
        assertEquals("auth-user-service", claims.getClaimAsString("iss"));
    }
}
