package auth_user_service.dto;

import auth_user_service.model.Role;
import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        Role role,
        boolean active,
        String fullName,
        String bio,
        String avatarUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserProfileResponse from(UserProfile profile) {
        User user = profile.getUser();
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                profile.getFullName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                user.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}