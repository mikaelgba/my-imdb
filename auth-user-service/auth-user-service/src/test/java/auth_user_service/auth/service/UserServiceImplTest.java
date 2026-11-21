package auth_user_service.auth.service;

import auth_user_service.dto.PagedResponse;
import auth_user_service.dto.UpdateUserStatusRequest;
import auth_user_service.dto.UserProfileResponse;
import auth_user_service.exception.ResourceNotFoundException;
import auth_user_service.model.Role;
import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import auth_user_service.repository.UserProfileRepository;
import auth_user_service.service.RefreshTokenService;
import auth_user_service.service.SecurityAuditService;
import auth_user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Deve desativar usuário e revogar todos os seus refresh tokens")
    void shouldDeactivateUserAndRevokeTokens() {
        UUID userId = UUID.randomUUID();
        User user = User.register("alvo@myimdb.com", "hash", Role.USER);
        UserProfile profile = new UserProfile(user, "Alvo Teste");

        when(userProfileRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(profile));

        UserProfileResponse response = userService.updateUserStatus(userId, new UpdateUserStatusRequest(false));

        assertFalse(response.active());
        verify(refreshTokenService).revokeAllUserTokens(user);
    }

    @Test
    @DisplayName("Deve listar usuários paginados")
    void shouldReturnPagedUsers() {
        User user = User.register("u1@myimdb.com", "hash", Role.USER);
        UserProfile profile = new UserProfile(user, "Usuario 1");
        var pageable = PageRequest.of(0, 10);

        when(userProfileRepository.findAllWithUser(pageable))
                .thenReturn(new PageImpl<>(List.of(profile), pageable, 1));

        PagedResponse<UserProfileResponse> result = userService.listAllUsers(pageable);

        assertEquals(1, result.totalElements());
        assertEquals("u1@myimdb.com", result.content().getFirst().email());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar perfil inexistente")
    void shouldThrowWhenProfileNotFound() {
        UUID missingId = UUID.randomUUID();
        when(userProfileRepository.findByUserIdWithUser(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfileByUserId(missingId));
    }

    @Test
    @DisplayName("Deve reativar usuário sem revogar tokens")
    void shouldActivateUserWithoutRevokingTokens() {
        UUID userId = UUID.randomUUID();
        User user = User.register("alvo@myimdb.com", "hash", Role.USER);
        user.deactivate();
        UserProfile profile = new UserProfile(user, "Alvo Teste");

        when(userProfileRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(profile));

        UserProfileResponse response = userService.updateUserStatus(userId, new UpdateUserStatusRequest(true));

        assertTrue(response.active());
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }
}
