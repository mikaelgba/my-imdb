package auth_user_service.controller;

import auth_user_service.dto.ChangePasswordRequest;
import auth_user_service.dto.UpdateProfileRequest;
import auth_user_service.dto.UserProfileResponse;
import auth_user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('USER', 'EDITOR', 'ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuário Autenticado", description = "Consulta e autogestão de perfil e senha do usuário logado")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','EDITOR','ADMIN')")
    @Operation(
            summary = "Obter perfil do usuário logado",
            description = "Retorna os dados do perfil associados ao token JWT autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil recuperado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return ResponseEntity.ok(userService.getProfileByUserId(userId));
    }

    @PatchMapping("/me/profile")
    @PreAuthorize("hasAnyRole('USER','EDITOR','ADMIN')")
    @Operation(
            summary = "Atualizar dados do perfil",
            description = "Atualiza parcialmente as informações cadastrais do usuário autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content)
    })
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PatchMapping("/me/password")
    @PreAuthorize("hasAnyRole('USER','EDITOR','ADMIN')")
    @Operation(
            summary = "Alterar senha",
            description = "Valida a senha atual e define uma nova senha para o usuário autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso (sem conteúdo no retorno)"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou nova senha fora do padrão", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado", content = @Content)
    })
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
