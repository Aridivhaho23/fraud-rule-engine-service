package com.example.fraud.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Passwords of the three built-in service accounts. They are REQUIRED (no defaults here): outside the
 * local {@code h2} profile they come from environment variables, so the application refuses to start
 * rather than running with a missing or empty password.
 *
 * <p>In a larger system these accounts would be replaced by an identity provider (OAuth2/OIDC).
 */
@Validated
@ConfigurationProperties(prefix = "fraud.security")
public record FraudSecurityProperties(
        @NotBlank String adminPassword,
        @NotBlank String analystPassword,
        @NotBlank String ingestionPassword) {
}
