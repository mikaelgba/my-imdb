package auth_user_service.config;

import auth_user_service.model.User;
import auth_user_service.model.UserProfile;
import auth_user_service.repository.UserProfileRepository;
import auth_user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSeedConfig {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    @Transactional
    public ApplicationRunner seedInitialAdmin() {

        return args -> {

            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                log.info("Seed de administrador inicial ignorado: variáveis APP_ADMIN_EMAIL/APP_ADMIN_PASSWORD não definidas.");
                return;
            }

            String normalizedEmail = adminEmail.trim().toLowerCase();

            // Já existe: isso para evitar toda vez um novo admin no banco
            if (userRepository.existsByEmail(normalizedEmail)) {
                return;
            }

            User admin = User.createInitialAdmin(normalizedEmail, passwordEncoder.encode(adminPassword));
            User savedAdmin = userRepository.save(admin);
            userProfileRepository.save(new UserProfile(savedAdmin, "Administrador do Sistema"));

            log.info("Administrador inicial criado com sucesso para o e-mail: {}", normalizedEmail);
        };
    }
}

