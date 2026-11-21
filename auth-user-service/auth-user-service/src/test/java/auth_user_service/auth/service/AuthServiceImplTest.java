package auth_user_service.auth.service;

import auth_user_service.dto.LoginRequest;
import auth_user_service.dto.RegisterRequest;
import auth_user_service.dto.TokenResponse;
import auth_user_service.dto.UserProfileResponse;
import auth_user_service.exception.AccountLockedException;
import auth_user_service.exception.EmailAlreadyExistsException;
import auth_user_service.exception.InvalidCredentialsException;
import auth_user_service.model.Role;
import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import auth_user_service.repository.UserProfileRepository;
import auth_user_service.repository.UserRepository;
import auth_user_service.service.RefreshTokenService;
import auth_user_service.service.SecurityAuditService;
import auth_user_service.service.TokenService;
import auth_user_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SecurityAuditService securityAuditService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Deve registrar usuário e perfil com sucesso")
    void shouldRegisterUserSuccessfully() {

        var request = new RegisterRequest("novo@myimdb.com", "Senha1234", "Novo Usuario", Role.EDITOR);
        when(userRepository.existsByEmail("novo@myimdb.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha1234")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = authService.register(request);

        assertEquals("novo@myimdb.com", response.email());
        assertEquals(Role.EDITOR, response.role());
        assertEquals("Novo Usuario", response.fullName());
    }

    @Test
    @DisplayName("Deve lançar EmailAlreadyExistsException quando e-mail já estiver cadastrado")
    void shouldThrowWhenEmailAlreadyExists() {

        var request = new RegisterRequest("duplicado@myimdb.com", "Senha1234", "Teste", Role.USER);
        when(userRepository.existsByEmail("duplicado@myimdb.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve autenticar usuário ativo e retornar par de tokens")
    void shouldLoginActiveUser() {

        User user = User.register("ativo@myimdb.com", "hashedPass", Role.USER);
        var loginReq = new LoginRequest("ativo@myimdb.com", "Senha1234");
        var expectedTokens = TokenResponse.of("jwt-access", "opaque-refresh", 900L);

        when(userRepository.findByEmail("ativo@myimdb.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha1234", "hashedPass")).thenReturn(true);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("opaque-refresh");
        when(tokenService.generateTokenPair(user, "opaque-refresh")).thenReturn(expectedTokens);

        TokenResponse result = authService.login(loginReq);

        assertEquals("jwt-access", result.accessToken());
        assertEquals("opaque-refresh", result.refreshToken());
    }

    @Test
    @DisplayName("Deve bloquear login de usuário desativado ou com senha incorreta")
    void shouldRejectLoginForInactiveUserOrWrongPassword() {

        User inactiveUser = User.register("inativo@myimdb.com", "hashedPass", Role.USER);
        inactiveUser.deactivate();

        when(userRepository.findByEmail("inativo@myimdb.com")).thenReturn(Optional.of(inactiveUser));

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("inativo@myimdb.com", "Senha1234"))
        );
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException quando e-mail não existir")
    void shouldThrowWhenEmailNotFoundOnLogin() {
        when(userRepository.findByEmail("naoexiste@myimdb.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("naoexiste@myimdb.com", "Senha1234"))
        );
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException quando a senha estiver incorreta")
    void shouldThrowWhenPasswordDoesNotMatch() {
        User user = User.register("user@myimdb.com", "hashedPass", Role.USER);
        when(userRepository.findByEmail("user@myimdb.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", "hashedPass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("user@myimdb.com", "SenhaErrada"))
        );
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Deve lançar AccountLockedException imediatamente se a conta estiver bloqueada")
    void shouldThrowAccountLockedExceptionWhenUserIsLocked() {
        User lockedUser = User.register("bloqueado@myimdb.com", "hashedPass", Role.USER);
        for (int i = 0; i < 5; i++) {
            lockedUser.registerFailedLogin();
        }

        when(userRepository.findByEmail("bloqueado@myimdb.com")).thenReturn(Optional.of(lockedUser));

        assertThrows(AccountLockedException.class, () ->
                authService.login(new LoginRequest("bloqueado@myimdb.com", "SenhaCorreta123"))
        );

        // Não deve nem gastar CPU validando BCrypt nem gerar token se a conta já está bloqueada
        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenService, never()).generateTokenPair(any(), any());
    }

    @Test
    @DisplayName("Deve registrar tentativa falha no SecurityAuditService quando a senha estiver incorreta")
    void shouldRecordFailedAttemptOnWrongPassword() {
        User user = User.register("user@myimdb.com", "hashedPass", Role.USER);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", java.util.UUID.randomUUID());

        when(userRepository.findByEmail("user@myimdb.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", "hashedPass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("user@myimdb.com", "SenhaErrada"))
        );

        verify(securityAuditService).recordFailedLoginAttempt(user.getId(), "user@myimdb.com");
    }
}
