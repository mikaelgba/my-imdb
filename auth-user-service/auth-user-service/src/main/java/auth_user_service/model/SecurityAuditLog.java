package auth_user_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "security_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "target_id")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SecurityAction action;

    @Column(length = 500)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public SecurityAuditLog(UUID actorId, UUID targetId, SecurityAction action,
                            String details, String ipAddress) {

        this.actorId = actorId;
        this.targetId = targetId;
        this.action = Objects.requireNonNull(action, "Ação de auditoria é obrigatória");
        this.details = details;
        this.ipAddress = ipAddress;
    }
}
