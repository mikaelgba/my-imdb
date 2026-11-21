package auth_user_service.service.impl;

import auth_user_service.config.RsaKeyProperties;
import auth_user_service.dto.TokenResponse;
import auth_user_service.model.User;
import auth_user_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final RsaKeyProperties rsaKeys;


    @Override
    public TokenResponse generateTokenPair(User user, String refreshToken) {
        Instant now = Instant.now();
        long expiresIn = rsaKeys.expirationSeconds();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-user-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("scope", user.getRole().name())
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return TokenResponse.of(accessToken, refreshToken, expiresIn);
    }
}