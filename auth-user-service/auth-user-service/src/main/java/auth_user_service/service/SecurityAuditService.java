package auth_user_service.service;

import auth_user_service.model.SecurityAction;
import java.util.UUID;

public interface SecurityAuditService {
    void recordEvent(UUID actorId, UUID targetId, SecurityAction action, String details);
    void recordFailedLoginAttempt(UUID userId, String email);
    void resetFailedLoginAttempts(UUID userId);
}
