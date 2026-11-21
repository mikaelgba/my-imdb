package auth_user_service.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String fullName,

        @Size(max = 500, message = "A bio deve ter no máximo 500 caracteres")
        String bio,

        @URL(message = "A URL do avatar deve ser válida")
        @Size(max = 255, message = "A URL do avatar deve ter no máximo 255 caracteres")
        String avatarUrl
) {}