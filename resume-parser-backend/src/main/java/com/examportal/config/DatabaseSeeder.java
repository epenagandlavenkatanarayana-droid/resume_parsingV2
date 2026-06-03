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

        String customAdminEmail = "epenagandlavenkat@gmail.com";
        User admin2 = userRepository.findByEmail(customAdminEmail).orElse(null);
        if (admin2 != null) {
            log.info("Found custom Admin user: {} with role: {}", customAdminEmail, admin2.getRole());
        }
        if (admin2 == null) {
            admin2 = User.builder()
                    .fullName("Venkat Epenagandla")
                    .email(customAdminEmail)
                    .phoneNumber("8888888888")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin2);
            log.info("Seeded custom Admin user: " + customAdminEmail);
        } else if (admin2.getRole() != Role.ADMIN) {
            admin2.setRole(Role.ADMIN);
            userRepository.save(admin2);
            log.info("Updated role to ADMIN for custom user: " + customAdminEmail);
        } else {
            log.info("Custom Admin user already exists in Firestore. Skipping seeding.");
        }

        String customAdminEmail2 = "epenagandlavenkaa@gmail.com";
        User admin3 = userRepository.findByEmail(customAdminEmail2).orElse(null);
        if (admin3 == null) {
            admin3 = User.builder()
                    .fullName("Venkat Epenagandla (Alt)")
                    .email(customAdminEmail2)
                    .phoneNumber("8888888888")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin3);
            log.info("Seeded custom Admin user: " + customAdminEmail2);
        } else if (admin3.getRole() != Role.ADMIN) {
            admin3.setRole(Role.ADMIN);
            userRepository.save(admin3);
            log.info("Updated role to ADMIN for custom user: " + customAdminEmail2);
        } else {
            log.info("Custom Admin user 2 already exists in Firestore. Skipping seeding.");
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
        User hr2 = userRepository.findByEmail(customHrEmail).orElse(null);
        if (hr2 != null) {
            log.info("Found custom HR user: {} with role: {}", customHrEmail, hr2.getRole());
        }
        if (hr2 == null) {
            hr2 = User.builder()
                    .fullName("Narayana Epenagandla")
                    .email(customHrEmail)
                    .phoneNumber("9999999999")
                    .password(passwordEncoder.encode("hr123"))
                    .role(Role.HR)
                    .build();
            userRepository.save(hr2);
            log.info("Seeded custom HR user: " + customHrEmail);
        } else if (hr2.getRole() != Role.HR) {
            hr2.setRole(Role.HR);
            userRepository.save(hr2);
            log.info("Updated role to HR for custom user: " + customHrEmail);
        } else {
            log.info("Custom HR user already exists in Firestore. Skipping seeding.");
        }
    }
}
