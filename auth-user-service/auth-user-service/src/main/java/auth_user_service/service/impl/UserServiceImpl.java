package auth_user_service.service.impl;

import auth_user_service.dto.*;
import auth_user_service.exception.InvalidCredentialsException;
import auth_user_service.exception.ResourceNotFoundException;
import auth_user_service.model.Role;
import auth_user_service.model.SecurityAction;
import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import auth_user_service.repository.UserProfileRepository;
import auth_user_service.service.RefreshTokenService;
import auth_user_service.service.SecurityAuditService;
import auth_user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenService refreshTokenService;
    private final SecurityAuditService securityAuditService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUserId(UUID userId) {
        UserProfile profile = findProfileOrThrow(userId);
        return UserProfileResponse.from(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = findProfileOrThrow(userId);
        profile.updateDetails(request.fullName(), request.bio(), request.avatarUrl());
        return UserProfileResponse.from(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> listAllUsers(Pageable pageable) {
        Page<UserProfileResponse> page = userProfileRepository.findAllWithUser(pageable)
                .map(UserProfileResponse::from);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserRole(UUID targetUserId, UpdateRoleRequest request) {
        UserProfile profile = findProfileOrThrow(targetUserId);
        Role oldRole = profile.getUser().getRole();
        profile.getUser().changeRole(request.role());

        securityAuditService.recordEvent(
                getCurrentActorId(),
                targetUserId,
                SecurityAction.ROLE_CHANGED,
                "Role alterada de " + oldRole + " para " + request.role()
        );

        return UserProfileResponse.from(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserStatus(UUID targetUserId, UpdateUserStatusRequest request) {
        UserProfile profile = findProfileOrThrow(targetUserId);
        User user = profile.getUser();

        if (Boolean.TRUE.equals(request.active())) {
            user.activate();
        } else {
            user.deactivate();
            refreshTokenService.revokeAllUserTokens(user);
        }

        securityAuditService.recordEvent(
                getCurrentActorId(),
                targetUserId,
                SecurityAction.STATUS_CHANGED,
                "Status alterado para active=" + request.active()
        );

        return UserProfileResponse.from(profile);
    }

    private UUID getCurrentActorId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        return null;
    }

    private UserProfile findProfileOrThrow(UUID userId) {
        return userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de usuário não encontrado para o ID: " + userId));
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserProfile profile = findProfileOrThrow(userId);
        User user = profile.getUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));

        // Revoga imediatamente todos os refresh tokens ativos (desconecta outros dispositivos)
        refreshTokenService.revokeAllUserTokens(user);

        securityAuditService.recordEvent(
                userId,
                userId,
                SecurityAction.PASSWORD_CHANGED,
                "Senha alterada pelo próprio usuário"
        );
    }
}