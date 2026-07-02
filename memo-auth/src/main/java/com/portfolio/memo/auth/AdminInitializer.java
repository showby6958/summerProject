package com.portfolio.memo.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "dev"}) // 운영(prod)에서는 절대 실행 안 됨
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;


    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[AdminInitializer] Admin already exist: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .name("ADMIN")
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        log.info("[AdminInitializer] Admin account created: {}", adminEmail);
    }
}
