package auth_user_service.auth.model;

import auth_user_service.model.Role;
import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileDomainTest {

    @Test
    @DisplayName("Deve criar perfil sanitizando espaços do nome completo")
    void shouldCreateProfileWithTrimmedName() {

        User user = User.register("user@myimdb.com", "hash", Role.USER);
        UserProfile profile = new UserProfile(user, "  Ana Silva  ");

        assertEquals("Ana Silva", profile.getFullName());
        assertNull(profile.getBio());
        assertNull(profile.getAvatarUrl());
    }

    @Test
    @DisplayName("Deve rejeitar nome nulo, em branco ou menor que 2 caracteres")
    void shouldRejectInvalidFullName() {

        User user = User.register("user@myimdb.com", "hash", Role.USER);

        assertThrows(NullPointerException.class, () -> new UserProfile(user, null));
        assertThrows(IllegalArgumentException.class, () -> new UserProfile(user, "   "));
        assertThrows(IllegalArgumentException.class, () -> new UserProfile(user, "A"));
    }

    @Test
    @DisplayName("Deve atualizar parcialmente apenas os campos não nulos no PATCH")
    void shouldPartiallyUpdateDetails() {

        User user = User.register("user@myimdb.com", "hash", Role.USER);
        UserProfile profile = new UserProfile(user, "Nome Inicial");

        profile.updateDetails(null, "  Minha bio  ", "  https://img.com  ");

        assertEquals("Nome Inicial", profile.getFullName());
        assertEquals("Minha bio", profile.getBio());
        assertEquals("https://img.com", profile.getAvatarUrl());

        // Atualizar apenas o nome não deveria apagar bio e avatar existentes do usuario
        profile.updateDetails("Novo Nome", null, null);
        assertEquals("Novo Nome", profile.getFullName());
        assertEquals("Minha bio", profile.getBio());
        assertEquals("https://img.com", profile.getAvatarUrl());
    }
}