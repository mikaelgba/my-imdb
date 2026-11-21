package auth_user_service.auth;

import auth_user_service.dto.*;
import auth_user_service.model.Role;

import auth_user_service.model.SecurityAction;
import auth_user_service.repository.SecurityAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityAuditLogRepository auditLogRepository;

    @Test
    @DisplayName("1. Fluxo completo: Cadastro EDITOR -> Login -> Refresh Rotation -> Bloqueio de Reuso")
    void shouldExecuteFullAuthAndRefreshRotationLifecycle() throws Exception {
        // 1. Registro como EDITOR
        var registerReq = new RegisterRequest("editor@myimdb.com", "SenhaForte123", "Maria Editora", Role.EDITOR);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("editor@myimdb.com"))
                .andExpect(jsonPath("$.role").value("EDITOR"));

        // 2. Login inicial
        var loginReq = new LoginRequest("editor@myimdb.com", "SenhaForte123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String firstRefreshToken = loginJson.get("refreshToken").asText();

        // 3. Rotacionar Refresh Token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(firstRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String secondRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("refreshToken").asText();

        // 4. Tentar reutilizar o primeiro Refresh Token (deve falhar com 401 e revogar o segundo)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(firstRefreshToken))))
                .andExpect(status().isUnauthorized());

        // 5. Segundo Refresh Token também deve ter sido revogado pela proteção de reuso
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(secondRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Logout: Deve revogar o Refresh Token impedindo novas renovações")
    void shouldInvalidateRefreshTokenOnLogout() throws Exception {
        // 1. Cadastra e faz login
        var registerReq = new RegisterRequest("logout.user@myimdb.com", "SenhaForte123", "Logout User", Role.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("logout.user@myimdb.com", "SenhaForte123"))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("refreshToken").asText();

        // 2. Realiza Logout (204 No Content)
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isNoContent());

        // 3. Tenta usar o Refresh Token após o logout (deve retornar 401 Unauthorized)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED
    )
    @DisplayName("3. Segurança: Deve bloquear conta (423 Locked) após 5 tentativas falhas e gravar log de auditoria")
    void shouldLockAccountAfterFiveFailedLoginAttemptsAndRecordAuditLog() throws Exception {
        // Usa e-mail dinâmico porque SecurityAuditServiceImpl usa REQUIRES_NEW (commit real no banco)
        String dynamicEmail = "ataque." + UUID.randomUUID() + "@myimdb.com";

        var registerReq = new RegisterRequest(dynamicEmail, "SenhaCorreta123", "Alvo Ataque", Role.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Realiza 5 tentativas com senha incorreta (todas retornam 401 Unauthorized)
        var wrongLogin = new LoginRequest(dynamicEmail, "SenhaErrada999");
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "192.168.1.50")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // Na 6ª tentativa, mesmo com a SENHA CORRETA, deve retornar 423 Locked
        var validLogin = new LoginRequest(dynamicEmail, "SenhaCorreta123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "192.168.1.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.title").value("Conta Temporariamente Bloqueada"));

        // Confirma que a auditoria registrou o bloqueio com o IP repassado pelo API Gateway
        boolean hasLockAudit = auditLogRepository.findAll().stream()
                .anyMatch(log -> log.getAction() == SecurityAction.ACCOUNT_LOCKED
                        && "192.168.1.50".equals(log.getIpAddress())
                        && log.getDetails() != null && log.getDetails().contains(dynamicEmail));
        assertTrue(hasLockAudit);
    }

    @Test
    @DisplayName("4. Fluxo Admin: Listar usuários paginados, promover role e desativar conta")
    void shouldAllowAdminToListPromoteAndDeactivateUsers() throws Exception {
        // 1. Cria usuário comum
        var userReq = new RegisterRequest("membro@myimdb.com", "SenhaForte123", "Membro Cinefilo", Role.USER);

        MvcResult createdRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String targetUserId = objectMapper.readTree(createdRes.getResponse().getContentAsString())
                .get("id").asText();

        // 2. Login do Admin (criado pelo AdminSeedConfig)
        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@seuprojeto.com", "Admin@123456"))))
                .andExpect(status().isOk())
                .andReturn();

        String adminAccessToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 3. Listagem paginada
        mockMvc.perform(get("/api/v1/admin/users?page=0&size=10")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());

        // 4. Promover para EDITOR
        mockMvc.perform(patch("/api/v1/admin/users/" + targetUserId + "/role")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.EDITOR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));

        // 5. Desativar usuário
        mockMvc.perform(patch("/api/v1/admin/users/" + targetUserId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 6. Usuário desativado não pode mais fazer login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("membro@myimdb.com", "SenhaForte123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. RBAC: Usuário comum ou EDITOR não deve acessar rotas administrativas (403 Forbidden)")
    void shouldForbidNonAdminFromAccessingAdminRoutes() throws Exception {
        var registerReq = new RegisterRequest("comum.rbac@myimdb.com", "SenhaForte123", "Usuario RBAC", Role.EDITOR);
        MvcResult createdRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = objectMapper.readTree(createdRes.getResponse().getContentAsString()).get("id").asText();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("comum.rbac@myimdb.com", "SenhaForte123"))))
                .andExpect(status().isOk())
                .andReturn();

        String editorToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Tentar listar usuários sendo EDITOR -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isForbidden());

        // Tentar alterar própria role para ADMIN -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Perfil: Usuário autenticado deve consultar e atualizar seu próprio perfil")
    void shouldGetAndUpdateOwnProfile() throws Exception {
        var registerReq = new RegisterRequest("perfil@myimdb.com", "SenhaForte123", "Nome Original", Role.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("perfil@myimdb.com", "SenhaForte123"))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Consulta GET /me
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil@myimdb.com"))
                .andExpect(jsonPath("$.fullName").value("Nome Original"));

        // Atualização parcial PATCH /me/profile
        var updateReq = new UpdateProfileRequest("Nome Atualizado", "Crítico de cinema", "https://myimdb.com");
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nome Atualizado"))
                .andExpect(jsonPath("$.bio").value("Crítico de cinema"))
                .andExpect(jsonPath("$.avatarUrl").value("https://myimdb.com"));
    }

    @Test
    @DisplayName("7. JWKS: Deve expor chave pública em /.well-known/jwks.json sem vazar parâmetros privados")
    void shouldExposePublicJwksEndpoint() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].e").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    @Test
    @DisplayName("8. Segurança Cadastro: Deve bloquear tentativa de cadastro direto com role ADMIN (400)")
    void shouldRejectDirectAdminRegistration() throws Exception {
        var adminAttempt = new RegisterRequest("invasor@myimdb.com", "SenhaForte123", "Tentativa Admin", Role.ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminAttempt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição Inválida"));
    }

    @Test
    @DisplayName("9. Validação: Deve retornar 400 Bad Request com mapa de erros quando payload for inválido")
    void shouldReturn400WhenRegisterPayloadIsInvalid() throws Exception {
        var invalidRequest = new RegisterRequest("email-invalido", "123", "", Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de Validação"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    @DisplayName("10. Autenticação: Deve retornar 401 Unauthorized sem token ou com token JWT adulterado")
    void shouldReturn401WhenAccessingProtectedEndpointsWithoutValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer token.jwt.adulterado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Conflito: Deve retornar 409 Conflict ao tentar cadastrar e-mail duplicado")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        var registerReq = new RegisterRequest("duplicado@myimdb.com", "SenhaForte123", "Usuario Um", Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        var duplicateReq = new RegisterRequest("DUPLICADO@myimdb.com", "OutraSenha123", "Usuario Dois", Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cadastro já registrado"));
    }

    @Test
    @DisplayName("12. Segurança: Deve alterar senha, revogar refresh tokens ativos e exigir nova senha no login")
    void shouldChangePasswordRevokeTokensAndRequireNewPassword() throws Exception {
        // 1. Cadastra e faz login
        var registerReq = new RegisterRequest("trocasenha@myimdb.com", "SenhaAntiga123", "User Senha", Role.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("trocasenha@myimdb.com", "SenhaAntiga123"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = tokens.get("accessToken").asString();
        String oldRefreshToken = tokens.get("refreshToken").asString();

        // 2. Troca a senha
        var changePassReq = new ChangePasswordRequest("SenhaAntiga123", "SenhaNovaForte456");
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePassReq)))
                .andExpect(status().isNoContent());

        // 3. Refresh token antigo deve ter sido revogado
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(oldRefreshToken))))
                .andExpect(status().isUnauthorized());

        // 4. Senha antiga não funciona mais, senha nova funciona
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("trocasenha@myimdb.com", "SenhaAntiga123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("trocasenha@myimdb.com", "SenhaNovaForte456"))))
                .andExpect(status().isOk());
    }
}
