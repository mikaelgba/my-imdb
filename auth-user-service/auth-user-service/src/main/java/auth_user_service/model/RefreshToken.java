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
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public RefreshToken(User user, String tokenHash, OffsetDateTime expiresAt) {
        this.user = Objects.requireNonNull(user, "Usuário não pode ser nulo");
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash não pode ser nulo");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Data de expiração não pode ser nula");
        this.revoked = false;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.revoked && !isExpired() && this.user.isActive();
    }

    public void revoke() {
        this.revoked = true;
    }
}
