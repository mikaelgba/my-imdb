package auth_user_service.controller;

import auth_user_service.dto.PagedResponse;
import auth_user_service.dto.UpdateRoleRequest;
import auth_user_service.dto.UpdateUserStatusRequest;
import auth_user_service.dto.UserProfileResponse;
import auth_user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administração (RBAC)", description = "Operações restritas a administradores para gestão de usuários, permissões e status")
public class AdminController {

    private final UserService userService;

    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista paginada de todos os usuários e seus respectivos perfis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado: requer permissão ADMIN", content = @Content)
    })
    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserProfileResponse>> listUsers(
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.listAllUsers(pageable));
    }

    @Operation(summary = "Alterar role de um usuário", description = "Promove ou rebaixa a permissão de acesso de um usuário (USER, EDITOR ou ADMIN).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Role informada inválida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado: requer permissão ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário alvo não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserProfileResponse> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserRole(id, request));
    }

    @Operation(summary = "Ativar ou desativar conta", description = "Altera o status ativo do usuário. Ao desativar (active=false), todos os Refresh Tokens do usuário são revogados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status da conta atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: requer permissão ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário alvo não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserProfileResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request));
    }
}