package auth_user_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 500)
    private String bio;

    //aqui invente a url que quiser, só Deus sabe quando vou implementar imagens no projeto
    @Column(name = "avatar_url")
    private String avatarUrl;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserProfile(User user, String fullName) {
        this.user = Objects.requireNonNull(user, "Usuário não pode ser nulo");
        this.fullName = validateFullName(fullName);
    }

    public void updateDetails(String fullName, String bio, String avatarUrl) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = validateFullName(fullName);
        }
        if (bio != null) {
            this.bio = bio.trim();
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl.trim();
        }
    }

    private String validateFullName(String name) {
        String sanitized = Objects.requireNonNull(name, "Nome completo é obrigatório").trim();
        if (sanitized.length() < 2) {
            throw new IllegalArgumentException("Nome completo deve ter ao menos 2 caracteres");
        }
        return sanitized;
    }
}
