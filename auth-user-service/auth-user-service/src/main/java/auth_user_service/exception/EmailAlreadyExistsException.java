package auth_user_service.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends DomainException {

    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "Cadastro já registrado", "O e-mail informado já está em uso: " + email);
    }
}
