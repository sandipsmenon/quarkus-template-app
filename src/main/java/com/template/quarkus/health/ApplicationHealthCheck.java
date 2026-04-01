package com.template.quarkus.health;

import com.template.quarkus.config.AppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * FEATURE: Custom Health Checks — Liveness check for the application itself.
 *
 * A liveness check answers: "Is the application healthy enough to keep running?"
 * If this returns DOWN, Kubernetes will restart the pod.
 *
 * Use liveness checks for things that are unrecoverable without a restart:
 *   - Deadlock detection
 *   - Required configuration validation
 *   - Thread pool exhaustion
 *
 * This example checks that required configuration is valid.
 * In a real app you might also:
 *   - Check that background threads are alive
 *   - Check for memory pressure (Runtime.getRuntime().freeMemory())
 *   - Verify internal state consistency
 */
@Liveness
@ApplicationScoped
public class ApplicationHealthCheck implements HealthCheck {

    @Inject
    AppConfig appConfig;

    @Override
    public HealthCheckResponse call() {
        // Validate that critical config values are sane
        boolean configValid = appConfig.defaultPageSize() > 0
                && appConfig.maxPageSize() >= appConfig.defaultPageSize();

        if (!configValid) {
            return HealthCheckResponse.named("application-config")
                    .down()
                    .withData("reason", "Invalid page size configuration")
                    .withData("defaultPageSize", appConfig.defaultPageSize())
                    .withData("maxPageSize", appConfig.maxPageSize())
                    .build();
        }

        Runtime runtime = Runtime.getRuntime();
        long freeMemoryMB  = runtime.freeMemory()  / (1024 * 1024);
        long totalMemoryMB = runtime.totalMemory() / (1024 * 1024);
        long usedMemoryMB  = totalMemoryMB - freeMemoryMB;

        return HealthCheckResponse.named("application-config")
                .up()
                .withData("defaultPageSize", appConfig.defaultPageSize())
                .withData("maxPageSize", appConfig.maxPageSize())
                .withData("usedMemoryMB", usedMemoryMB)
                .withData("totalMemoryMB", totalMemoryMB)
                .build();
    }
}
