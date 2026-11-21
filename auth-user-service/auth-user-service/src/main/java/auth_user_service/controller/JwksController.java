package auth_user_service.controller;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JWKSet jwkSet;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getJwks() {
        return jwkSet.toPublicJWKSet().toJSONObject();
    }
}
