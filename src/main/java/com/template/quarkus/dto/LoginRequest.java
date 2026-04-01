package com.template.quarkus.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEATURE: JWT Security — Login request payload.
 *
 * The client sends username/password to POST /api/auth/login.
 * In production, validate against a database or LDAP.
 * This demo uses hardcoded in-memory users for illustration.
 */
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "admin", required = true)
    public String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password", example = "admin123", required = true)
    public String password;
}
