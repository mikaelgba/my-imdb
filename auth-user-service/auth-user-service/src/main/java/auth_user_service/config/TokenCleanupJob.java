package auth_user_service.config;

import auth_user_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(7);
        int deletedCount = refreshTokenRepository.deleteAllExpiredBefore(cutoffDate);

        if (deletedCount > 0) {
            log.info("Limpeza automática concluída: {} refresh tokens expirados foram removidos.", deletedCount);
        }
    }
}
