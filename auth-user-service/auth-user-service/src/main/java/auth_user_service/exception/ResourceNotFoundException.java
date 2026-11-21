package auth_user_service.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Recurso Não Encontrado", message);
    }
}
