package com.examportal.config;

import com.examportal.entity.User;
import com.examportal.enums.Role;
import com.examportal.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("DatabaseSeeder starting. Seeding default accounts in Firestore...");
        seedAdminUser();
        seedHrUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@example.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email(adminEmail)
                    .phoneNumber("1234567890")
                    .password(passwordEncoder.encode("admin123")) // Dummy password as login is OTP-only
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default Admin user: admin@example.com");
        } else {
            log.info("Admin user already exists in Firestore. Skipping seeding.");
        }
    }

    private void seedHrUser() {
        String hrEmail = "hr@example.com";
        if (!userRepository.existsByEmail(hrEmail)) {
            User hr = User.builder()
                    .fullName("HR Representative")
                    .email(hrEmail)
                    .phoneNumber("0987654321")
                    .password(passwordEncoder.encode("hr123")) // Dummy password as login is OTP-only
                    .role(Role.HR)
                    .build();
            userRepository.save(hr);
            log.info("Seeded default HR user: hr@example.com");
        } else {
            log.info("HR user already exists in Firestore. Skipping seeding.");
        }

        String customHrEmail = "narayanaepenagandla@gmail.com";
        if (!userRepository.existsByEmail(customHrEmail)) {
            User hr = User.builder()
                    .fullName("Narayana Epenagandla")
                    .email(customHrEmail)
                    .phoneNumber("9999999999")
                    .password(passwordEncoder.encode("hr123"))
                    .role(Role.HR)
                    .build();
            userRepository.save(hr);
            log.info("Seeded custom HR user: " + customHrEmail);
        } else {
            log.info("Custom HR user already exists in Firestore. Skipping seeding.");
        }

        String customHrEmail2 = "epenagandlavenkat@gmail.com";
        if (!userRepository.existsByEmail(customHrEmail2)) {
            User hr2 = User.builder()
                    .fullName("Venkat Epenagandla")
                    .email(customHrEmail2)
                    .phoneNumber("8888888888")
                    .password(passwordEncoder.encode("hr123"))
                    .role(Role.HR)
                    .build();
            userRepository.save(hr2);
            log.info("Seeded custom HR user: " + customHrEmail2);
        } else {
            log.info("Custom HR user 2 already exists in Firestore. Skipping seeding.");
        }
    }
}
