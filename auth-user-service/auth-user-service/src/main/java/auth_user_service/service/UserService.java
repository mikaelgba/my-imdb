package auth_user_service.service;

import auth_user_service.dto.*;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface UserService {
    UserProfileResponse getProfileByUserId(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);
    PagedResponse<UserProfileResponse> listAllUsers(Pageable pageable);
    UserProfileResponse updateUserRole(UUID targetUserId, UpdateRoleRequest request);
    UserProfileResponse updateUserStatus(UUID targetUserId, UpdateUserStatusRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
}