package auth_user_service.repository;

import auth_user_service.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query("SELECT p FROM UserProfile p JOIN FETCH p.user WHERE p.userId = :userId")
    Optional<UserProfile> findByUserIdWithUser(UUID userId);

    @Query(
            value = "SELECT p FROM UserProfile p JOIN FETCH p.user",
            countQuery = "SELECT COUNT(p) FROM UserProfile p"
    )
    Page<UserProfile> findAllWithUser(Pageable pageable);
}