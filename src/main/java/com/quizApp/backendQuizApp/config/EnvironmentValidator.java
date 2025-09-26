package com.quizApp.backendQuizApp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentValidator {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
   public void validateEnvironment() {
        validateVariable("MONGO_URI", mongoUri);
        validateVariable("GEMINI_API_KEY", geminiApiKey);
        validateVariable("JWT_SECRET", jwtSecret);

        // Additional validation for JWT secret length
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET is too short. It should be at least 32 characters long for security.");
        }
    }

    private void validateVariable(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                String.format("Required environment variable %s is not set", name));
        }
        
        // Check if the value still contains the placeholder
        if (value.startsWith("${") && value.endsWith("}")) {
            throw new IllegalStateException(
                String.format("Environment variable %s is not properly configured", name));
        }
    }
}