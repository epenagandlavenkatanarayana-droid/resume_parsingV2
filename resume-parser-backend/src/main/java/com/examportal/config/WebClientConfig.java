package com.examportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Explicitly registers WebClient.Builder as a Spring bean.
 * 
 * This is required because the project uses both spring-boot-starter-web (Servlet/MVC)
 * and spring-boot-starter-webflux (for WebClient HTTP calls to Gemini API).
 * Without this, Spring Boot may fail to inject WebClient.Builder into
 * ResumeParserServiceImpl in certain deployment environments (e.g., Render).
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
