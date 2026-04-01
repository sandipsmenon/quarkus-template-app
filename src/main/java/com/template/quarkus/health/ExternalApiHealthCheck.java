package com.template.quarkus.health;

import com.template.quarkus.client.JsonPlaceholderClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * FEATURE: Custom Health Checks — Readiness check for the external API dependency.
 *
 * Quarkus SmallRye Health exposes three MicroProfile Health endpoints:
 *   GET /q/health       — combined liveness + readiness
 *   GET /q/health/live  — liveness: "is the app running?"
 *   GET /q/health/ready — readiness: "is the app ready to serve traffic?"
 *
 * You can create custom checks by implementing HealthCheck and annotating with:
 *   @Liveness   — for liveness probes (Kubernetes restarts the pod if this fails)
 *   @Readiness  — for readiness probes (Kubernetes stops routing traffic if this fails)
 *   @Startup    — for startup probes (checked once during container startup)
 *
 * Quarkus also auto-includes checks for:
 *   - Database connectivity (quarkus-hibernate-orm-panache auto-registers)
 *   - Disk space
 *   - JVM memory usage
 *
 * This check verifies that the external JSONPlaceholder API is reachable.
 * It is marked @Readiness: if the external API is down, the pod won't
 * receive traffic — preventing cascading failures in dependent services.
 */
@Readiness
@ApplicationScoped
public class ExternalApiHealthCheck implements HealthCheck {

    @RestClient
    @Inject
    JsonPlaceholderClient jsonPlaceholderClient;

    @Override
    public HealthCheckResponse call() {
        try {
            // Make a lightweight probe call — fetching a single post is cheap
            jsonPlaceholderClient.getPostById(1);

            return HealthCheckResponse.named("external-api")
                    .up()
                    .withData("url", "https://jsonplaceholder.typicode.com")
                    .withData("status", "reachable")
                    .build();

        } catch (Exception e) {
            return HealthCheckResponse.named("external-api")
                    .down()
                    .withData("url", "https://jsonplaceholder.typicode.com")
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
