package com.example.urlshortener.common.config;

import com.example.urlshortener.common.model.AdminUser;
import com.example.urlshortener.common.model.FeatureFlag;
import com.example.urlshortener.common.repository.AdminUserRepository;
import com.example.urlshortener.common.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final FeatureFlagRepository featureFlagRepository;

    @Override
    public void run(String... args) throws Exception {
        if (adminUserRepository.count() == 0) {
            log.info("No admin users found. Seeding default admin account...");
            AdminUser admin = AdminUser.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("password"))
                    .build();
            adminUserRepository.save(admin);
            log.info("Default admin account created (username: admin, password: password). Please change this in production!");
        }

        if (featureFlagRepository.findByName("MALICIOUS_URL_DETECTION").isEmpty()) {
            log.info("Seeding default feature flag: MALICIOUS_URL_DETECTION");
            FeatureFlag flag = FeatureFlag.builder()
                    .name("MALICIOUS_URL_DETECTION")
                    .enabled(true)
                    .description("Enables integration with Google Safe Browsing to block malware URLs.")
                    .build();
            featureFlagRepository.save(flag);
        }
    }
}
