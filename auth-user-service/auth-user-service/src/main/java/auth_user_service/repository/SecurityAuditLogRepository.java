package auth_user_service.repository;

import auth_user_service.model.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {
}
