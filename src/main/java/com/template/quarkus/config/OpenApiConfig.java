package com.template.quarkus.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
    info = @Info(
        title = "Quarkus Template API",
        version = "2.0.0",
        description = """
            A comprehensive Quarkus reference guide demonstrating:

            **Basic Features:**
            - Full CRUD REST API with PostgreSQL via Panache ORM
            - Bean Validation, global exception handling, consistent error responses
            - MapStruct DTO mapping, pagination, search, soft delete
            - External REST client with type-safe interface (@RegisterRestClient)

            **Intermediate Features:**
            - **Caching**: @CacheResult / @CacheInvalidate with Caffeine (5-min TTL)
            - **Type-safe Config**: @ConfigMapping with nested config groups
            - **CDI Events**: fire-and-forget product lifecycle events
            - **Scheduled Tasks**: @Scheduled with cron and interval expressions
            - **Custom Health Checks**: @Readiness + @Liveness implementations
            - **JAX-RS Filters**: request/response logging filter

            **Advanced Features:**
            - **JWT Security**: SmallRye JWT — login, sign tokens, @RolesAllowed
            - **Fault Tolerance**: @Retry + @Timeout + @CircuitBreaker + @Fallback
            - **CDI Interceptors**: custom @Logged and @Timed AOP-style interceptors
            - **WebSockets**: real-time product notifications via websockets-next
            - **Reactive Programming**: Mutiny Uni/Multi for async and streaming
            - **Server-Sent Events**: SSE streaming with Multi
            - **Multipart File Upload**: multipart/form-data with FileUpload

            ---
            **Authentication:** Use `POST /api/auth/login` to get a Bearer token.
            Demo credentials: `admin/admin123` (Admin) or `user/user123` (User)
            """,
        contact = @Contact(
            name = "Template Support",
            email = "support@template.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development server")
    },
    tags = {
        @Tag(name = "Products",          description = "Product CRUD — basic REST, caching, events, security"),
        @Tag(name = "Authentication",    description = "JWT login — get a Bearer token for protected endpoints"),
        @Tag(name = "Reactive Products", description = "Mutiny Uni/Multi reactive programming patterns"),
        @Tag(name = "File Upload",       description = "Multipart file upload demonstration"),
        @Tag(name = "External API",      description = "REST client with fault tolerance (retry, circuit breaker, fallback)")
    },
    components = @Components(
        securitySchemes = @SecurityScheme(
            securitySchemeName = "bearerAuth",
            type               = SecuritySchemeType.HTTP,
            scheme             = "bearer",
            bearerFormat       = "JWT",
            description        = "JWT Bearer token. Get one from POST /api/auth/login"
        )
    )
)
public class OpenApiConfig extends Application {
}
