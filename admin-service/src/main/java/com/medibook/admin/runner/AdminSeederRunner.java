package com.medibook.admin.runner;

import com.medibook.admin.config.AdminConfig;
import com.medibook.admin.entity.User;
import com.medibook.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs ONCE at application startup.
 *
 * Reads every entry under `app.admins` in application.yml and inserts
 * them into the `users` table (auth_db) with role = "Admin" if they
 * don't already exist.
 *
 * This means:
 *  - harshalchoudhary340@gmail.com / #Harshal@123  → seeded as Admin
 *  - adityalandge64@gmail.com       / #Harsh@123   → seeded as Admin
 *
 * Both can then log in via the existing  POST /auth/login  endpoint
 * and receive a JWT with role "Admin" — giving them full admin access.
 *
 * To add more admins in the future, just add another entry under
 * `app.admins` in application.yml and restart admin-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeederRunner implements ApplicationRunner {

    private final AdminConfig adminConfig;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (adminConfig.getAdmins() == null || adminConfig.getAdmins().isEmpty()) {
            log.warn("[AdminSeeder] No admins configured in application.yml — skipping seed.");
            return;
        }

        for (AdminConfig.AdminEntry entry : adminConfig.getAdmins()) {
            if (userRepository.existsByEmail(entry.getEmail())) {
                log.info("[AdminSeeder] Admin already exists — skipping: {}", entry.getEmail());
                continue;
            }

            User admin = User.builder()
                    .fullName(entry.getFullName())
                    .email(entry.getEmail())
                    .passwordHash(passwordEncoder.encode(entry.getPassword()))
                    .role("Admin")
                    .phone("")
                    .isActive(true)
                    .provider(null)
                    .build();

            userRepository.save(admin);
            log.info("[AdminSeeder] ✅ Admin seeded successfully: {} <{}>",
                    entry.getFullName(), entry.getEmail());
        }
    }
}
