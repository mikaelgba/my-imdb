package auth_user_service.controller;

import auth_user_service.dto.*;
import auth_user_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos para cadastro, login, renovação de sessão e logout")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Cadastrar novo usuário", description = "Cria uma conta com perfil USER (padrão) ou EDITOR. Cadastros diretos como ADMIN são bloqueados.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou tentativa de cadastro como ADMIN",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado no sistema",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Realizar login", description = "Autentica o usuário e emite um par de tokens (Access Token JWT + Refresh Token opaco).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload de login inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou conta desativada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "423", description = "Conta temporariamente bloqueada após 5 tentativas falhas",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Renovar tokens (Rotation)", description = "Valida o Refresh Token atual, revoga-o imediatamente e emite um novo par de tokens. Detecta reuso de tokens antigos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Refresh Token inválido, expirado ou já utilizado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Operation(summary = "Encerrar sessão (Logout)", description = "Revoga o Refresh Token informado para impedir futuras renovações.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Refresh Token não informado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
