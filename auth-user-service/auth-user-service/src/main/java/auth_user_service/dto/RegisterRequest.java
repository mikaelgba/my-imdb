package auth_user_service.dto;

import auth_user_service.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String password,

        @NotBlank(message = "O nome completo é obrigatório")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String fullName,

        Role role
) {}
