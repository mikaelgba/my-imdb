package auth_user_service.auth.model;

import auth_user_service.model.RefreshToken;
import auth_user_service.model.Role;
import auth_user_service.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenDomainTest {

    @Test
    @DisplayName("Token deve ser válido quando não revogado, não expirado e usuário ativo")
    void shouldBeValidWhenActiveAndNotExpired() {

        User user = User.register("user@email.com", "hash", Role.USER);
        RefreshToken token = new RefreshToken(user, "tokenHash", OffsetDateTime.now().plusHours(1));

        assertTrue(token.isValid());
        assertFalse(token.isExpired());
    }

    @Test
    @DisplayName("Token deve ser inválido quando expirado, revogado ou usuário desativado")
    void shouldBeInvalidUnderFailureConditions() {

        User user = User.register("user@email.com", "hash", Role.USER);

        RefreshToken expired = new RefreshToken(user, "hash1", OffsetDateTime.now().minusMinutes(1));
        assertFalse(expired.isValid());

        RefreshToken revoked = new RefreshToken(user, "hash2", OffsetDateTime.now().plusHours(1));
        revoked.revoke();
        assertFalse(revoked.isValid());

        RefreshToken validToken = new RefreshToken(user, "hash3", OffsetDateTime.now().plusHours(1));
        user.deactivate();
        assertFalse(validToken.isValid());
    }
}
