package com.template.quarkus.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

/**
 * FEATURE: JWT Security — token response returned after successful login.
 *
 * The client should include the token in subsequent requests:
 *   Authorization: Bearer <token>
 */
@Schema(description = "JWT authentication token response")
public class TokenResponse {

    @Schema(description = "JWT bearer token", required = true)
    public String token;

    @Schema(description = "Token type (always Bearer)", example = "Bearer")
    public String tokenType = "Bearer";

    @Schema(description = "Token expiry in seconds", example = "3600")
    public long expiresIn;

    @Schema(description = "Roles granted to this user")
    public Set<String> roles;

    public TokenResponse(String token, long expiresIn, Set<String> roles) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.roles = roles;
    }
}
