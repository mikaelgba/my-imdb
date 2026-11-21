package auth_user_service.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "O status 'active' é obrigatório")
        Boolean active
) {}
