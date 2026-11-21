package auth_user_service.controller;

import auth_user_service.dto.ChangePasswordRequest;
import auth_user_service.dto.UpdateProfileRequest;
import auth_user_service.dto.UserProfileResponse;
import auth_user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return ResponseEntity.ok(userService.getProfileByUserId(userId));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
