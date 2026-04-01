package com.template.quarkus.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(
    info = @Info(
        title = "Quarkus Template API",
        version = "1.0.0",
        description = """
            A production-quality Quarkus reference template demonstrating:
            - Full CRUD REST API with PostgreSQL via Panache ORM
            - External API integration (JSONPlaceholder) with data transformation
            - Bean Validation, global exception handling, and consistent error responses
            - MapStruct DTO mapping
            - Micrometer metrics and SmallRye Health checks
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
        @Tag(name = "Products", description = "Product catalog management endpoints"),
        @Tag(name = "External API", description = "Endpoints that proxy and transform data from jsonplaceholder.typicode.com")
    }
)
public class OpenApiConfig extends Application {
}
