package auth_user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Falha na Autenticação do Login", "E-mail ou senha inválidos, ou conta desativada.");
    }
}
