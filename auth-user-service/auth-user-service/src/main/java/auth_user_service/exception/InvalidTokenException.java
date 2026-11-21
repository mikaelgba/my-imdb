package auth_user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends DomainException {

    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "Token Inválido ou Expirado", message);
    }
}
