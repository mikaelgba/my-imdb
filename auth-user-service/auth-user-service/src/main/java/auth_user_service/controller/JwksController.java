package auth_user_service.controller;

import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Chaves Públicas (JWKS)", description = "Descoberta de chaves públicas RSA para validação distribuída de tokens JWT")
public class JwksController {

    private final JWKSet jwkSet;

    @Operation(summary = "Obter JSON Web Key Set (JWKS)", description = "Retorna as chaves públicas RSA no padrão RFC 7517 para consumo pelo API Gateway e demais microsserviços.")
    @ApiResponse(responseCode = "200", description = "Chave pública retornada com sucesso")
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getJwks() {
        return jwkSet.toPublicJWKSet().toJSONObject();
    }
}
