package auth_user_service.service.impl;

import auth_user_service.model.SecurityAction;
import auth_user_service.model.SecurityAuditLog;
import auth_user_service.repository.SecurityAuditLogRepository;
import auth_user_service.repository.UserRepository;
import auth_user_service.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID actorId, UUID targetId, SecurityAction action, String details) {
        String clientIp = resolveClientIp();
        SecurityAuditLog logEntry = new SecurityAuditLog(actorId, targetId, action, details, clientIp);
        auditLogRepository.save(logEntry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLoginAttempt(UUID userId, String email) {
        String clientIp = resolveClientIp();

        userRepository.findById(userId).ifPresent(user -> {
            user.registerFailedLogin();
            userRepository.save(user);

            SecurityAction action = user.isAccountLocked()
                    ? SecurityAction.ACCOUNT_LOCKED
                    : SecurityAction.LOGIN_FAILED;

            String details = user.isAccountLocked()
                    ? "Conta bloqueada após " + user.getFailedLoginAttempts() + " tentativas falhas para: " + email
                    : "Tentativa falha (" + user.getFailedLoginAttempts() + ") para: " + email;

            auditLogRepository.save(new SecurityAuditLog(userId, userId, action, details, clientIp));
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedLoginAttempts(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.resetFailedLogins();
                userRepository.save(user);
            }
        });
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "SYSTEM";

            HttpServletRequest request = attributes.getRequest();
            // Prioriza o IP real repassado pelo API Gateway / Load Balancer
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
