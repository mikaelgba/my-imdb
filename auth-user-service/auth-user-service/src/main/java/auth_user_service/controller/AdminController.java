package auth_user_service.controller;

import auth_user_service.dto.PagedResponse;
import auth_user_service.dto.UpdateRoleRequest;
import auth_user_service.dto.UpdateUserStatusRequest;
import auth_user_service.dto.UserProfileResponse;
import auth_user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserProfileResponse>> listUsers(
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.listAllUsers(pageable));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserProfileResponse> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserRole(id, request));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserProfileResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request));
    }
}