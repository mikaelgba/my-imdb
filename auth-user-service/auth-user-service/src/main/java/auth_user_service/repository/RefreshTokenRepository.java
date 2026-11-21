package auth_user_service.repository;

import auth_user_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashWithUser(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllActiveTokensByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoffDate")
    int deleteAllExpiredBefore(java.time.OffsetDateTime cutoffDate);
}
