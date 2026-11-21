package auth_user_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private User(String email, String passwordHash, Role role) {
        this.email = Objects.requireNonNull(email, "Email não pode ser nulo").trim().toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash não pode ser nulo");
        this.role = Objects.requireNonNull(role, "Role não pode ser nulo");
        this.active = true;
    }

    public static User register(String email, String encodedPassword, Role requestedRole) {
        Role assignedRole = (requestedRole == null) ? Role.USER : requestedRole;

        if (assignedRole == Role.ADMIN) {
            throw new IllegalArgumentException("Não é permitido criar uma conta diretamente com a role ADMIN.");
        }

        return new User(email, encodedPassword, assignedRole);
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "Nova role não pode ser nulo");
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public static User createInitialAdmin(String email, String encodedPassword) {
        return new User(email, encodedPassword, Role.ADMIN);
    }

    public boolean isAccountLocked() {
        return this.lockedUntil != null && OffsetDateTime.now().isBefore(this.lockedUntil);
    }

    public void registerFailedLogin() {
        // Se um bloqueio anterior já expirou, reinicia a contagem antes de somar
        if (this.lockedUntil != null && OffsetDateTime.now().isAfter(this.lockedUntil)) {
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
        }

        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = OffsetDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void changePassword(String newEncodedPassword) {
        String sanitized = Objects.requireNonNull(newEncodedPassword, "Nova senha codificada não pode ser nula");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Nova senha codificada não pode estar em branco");
        }
        this.passwordHash = sanitized;
    }
}