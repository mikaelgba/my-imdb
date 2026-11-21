package auth_user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected DomainException(HttpStatus status, String title, String message) {
        super(message);
        this.status = status;
        this.title = title;
    }
}