package auth_user_service.dto;

import auth_user_service.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "A role é obrigatória")
        Role role
) {}
