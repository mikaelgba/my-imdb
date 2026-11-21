package auth_user_service.auth.model;

import auth_user_service.model.Role;
import auth_user_service.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserDomainTest {

    @Test
    @DisplayName("Deve criar usuário com role USER quando role informada for nula")
    void shouldDefaultToUserRoleWhenNull() {
        User user = User.register("  Teste@Email.COM  ", "hash123", null);

        assertEquals("teste@email.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Deve permitir criar usuário com role EDITOR")
    void shouldAllowEditorRegistration() {
        User user = User.register("editor@email.com", "hash123", Role.EDITOR);

        assertEquals(Role.EDITOR, user.getRole());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar registrar diretamente como ADMIN")
    void shouldRejectAdminRegistration() {
        assertThrows(IllegalArgumentException.class, () ->
                User.register("admin@email.com", "hash123", Role.ADMIN)
        );
    }

    @Test
    @DisplayName("Deve alternar status ativo/inativo e alterar role corretamente")
    void shouldChangeStatusAndRole() {
        User user = User.register("user@email.com", "hash123", Role.USER);

        user.deactivate();
        assertFalse(user.isActive());

        user.activate();
        assertTrue(user.isActive());

        assertNotNull(user.getRole());
        user.changeRole(Role.EDITOR);
        assertEquals(Role.EDITOR, user.getRole());
    }

    @Test
    @DisplayName("Deve bloquear conta após 5 tentativas de login falhas e desbloquear após reset")
    void shouldLockAccountAfterFiveFailedAttemptsAndResetOnSuccess() {

        User user = User.register("alvo@myimdb.com", "hash123", Role.USER);

        // Primeiras 4 tentativas falhas não devem bloquear a conta
        for (int i = 1; i <= 4; i++) {
            user.registerFailedLogin();
            assertEquals(i, user.getFailedLoginAttempts());
            assertFalse(user.isAccountLocked());
        }

        // 5ª tentativa falha deve bloquear a conta
        user.registerFailedLogin();
        assertEquals(5, user.getFailedLoginAttempts());
        assertTrue(user.isAccountLocked());
        assertNotNull(user.getLockedUntil());

        // Reset após login bem-sucedido deve limpar contador e bloqueio
        user.resetFailedLogins();
        assertEquals(0, user.getFailedLoginAttempts());
        assertFalse(user.isAccountLocked());
        assertNull(user.getLockedUntil());
    }

    @Test
    @DisplayName("Deve reiniciar contagem de falhas se o tempo de bloqueio anterior já tiver expirado")
    void shouldRestartFailedAttemptsCountAfterLockExpires() {

        User user = User.register("expirado@myimdb.com", "hash123", Role.USER);

        // Simula um bloqueio que já venceu há 1 minuto
        org.springframework.test.util.ReflectionTestUtils.setField(user, "failedLoginAttempts", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(
                user, "lockedUntil", java.time.OffsetDateTime.now().minusMinutes(1)
        );

        assertFalse(user.isAccountLocked());

        // Uma nova falha deve reiniciar o contador para 1, em vez de ir para 6 e bloquear novamente
        user.registerFailedLogin();
        assertEquals(1, user.getFailedLoginAttempts());
        assertFalse(user.isAccountLocked());
    }
}
