package auth_user_service.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends DomainException {

    public AccountLockedException() {
        super(
                HttpStatus.LOCKED,
                "Conta Temporariamente Bloqueada",
                "Múltiplas tentativas de login falhas detectadas. Tente novamente em 15 minutos."
        );
    }
}
