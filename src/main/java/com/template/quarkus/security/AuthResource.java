package com.template.quarkus.security;

import com.template.quarkus.dto.LoginRequest;
import com.template.quarkus.dto.TokenResponse;
import com.template.quarkus.exception.ErrorResponse;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * FEATURE: JWT Security — Authentication endpoint.
 *
 * How JWT auth works in Quarkus:
 * 1. Client sends credentials to POST /api/auth/login
 * 2. Server validates credentials and builds a signed JWT
 * 3. Client includes the token in subsequent requests:
 *      Authorization: Bearer <token>
 * 4. Quarkus automatically verifies the JWT signature using publicKey.pem
 * 5. Endpoints annotated with @RolesAllowed restrict access by role
 *
 * Key annotations:
 *   @PermitAll    — anyone can call this endpoint (no token needed)
 *   @Authenticated — any valid token required (no specific role)
 *   @RolesAllowed("Admin") — only users with "Admin" role
 *
 * Demo users (in production, store users in a database):
 *   admin / admin123 → roles: [User, Admin]
 *   user  / user123  → roles: [User]
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "JWT authentication — login to receive a Bearer token")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    // Token lifetime (1 hour)
    private static final long TOKEN_EXPIRY_SECONDS = 3600L;

    // Demo in-memory user store: username → { password, roles }
    // In production: replace with a UserService that queries a database
    private static final Map<String, DemoUser> USERS = Map.of(
        "admin", new DemoUser("admin123", Set.of("User", "Admin")),
        "user",  new DemoUser("user123",  Set.of("User"))
    );

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------
    @POST
    @Path("/login")
    @PermitAll
    @Operation(
        summary = "Authenticate and obtain a JWT",
        description = """
            Submit username and password to receive a signed JWT.
            Include the token in subsequent requests:
              Authorization: Bearer <token>

            **Demo credentials:**
            - admin / admin123 → roles: User, Admin
            - user  / user123  → roles: User
            """
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Login successful — JWT returned",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @APIResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @APIResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response login(@Valid LoginRequest request) {
        LOG.debugf("Login attempt for username=%s", request.username);

        DemoUser user = USERS.get(request.username);
        if (user == null || !user.password().equals(request.password)) {
            LOG.warnf("Failed login attempt for username=%s", request.username);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Invalid username or password"))
                    .build();
        }

        // Build a signed JWT using SmallRye JWT Builder
        // The private key is loaded from privateKey.pem (configured via smallrye.jwt.sign.key.location)
        String token = Jwt
                .issuer("https://quarkus-template-app")   // must match mp.jwt.verify.issuer
                .upn(request.username)                     // "user principal name" claim
                .groups(user.roles())                      // maps to roles for @RolesAllowed
                .expiresIn(Duration.ofSeconds(TOKEN_EXPIRY_SECONDS))
                .sign();

        LOG.infof("Successful login for username=%s, roles=%s", request.username, user.roles());
        return Response.ok(new TokenResponse(token, TOKEN_EXPIRY_SECONDS, user.roles())).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me  — example of a protected endpoint
    // -------------------------------------------------------------------------
    @GET
    @Path("/me")
    @Operation(
        summary = "Get current user info",
        description = "Returns info about the currently authenticated user. Requires a valid JWT."
    )
    @APIResponse(responseCode = "200", description = "Current user info")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public Response me(@jakarta.ws.rs.core.Context jakarta.ws.rs.core.SecurityContext secCtx) {
        if (secCtx.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(Map.of(
            "username", secCtx.getUserPrincipal().getName(),
            "isAdmin", secCtx.isUserInRole("Admin")
        )).build();
    }

    /** Simple record to hold demo user data. Replace with a DB entity in production. */
    private record DemoUser(String password, Set<String> roles) {}
}
