package com.examportal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;
            String firebaseCreds = System.getenv("FIREBASE_CREDENTIALS");
            if (firebaseCreds != null && !firebaseCreds.trim().isEmpty()) {
                if (firebaseCreds.endsWith(".json")) {
                    serviceAccount = new java.io.FileInputStream(firebaseCreds);
                } else {
                    serviceAccount = new java.io.ByteArrayInputStream(firebaseCreds.getBytes("UTF-8"));
                }
            } else {
                serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
