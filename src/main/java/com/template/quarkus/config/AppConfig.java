package com.template.quarkus.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * FEATURE: Type-safe Configuration with @ConfigMapping.
 *
 * @ConfigMapping binds application.properties keys with the "app." prefix
 * to this interface. Quarkus generates a CDI bean that you can @Inject anywhere.
 *
 * Benefits over @ConfigProperty:
 *  - Grouped and structured config (nested interfaces)
 *  - Validated at startup — missing required values fail fast
 *  - Immutable and type-safe — no raw string access
 *  - IDE-friendly with auto-completion
 *
 * Usage:
 *   @Inject AppConfig config;
 *   int pageSize = config.defaultPageSize();
 *   long timeout = config.externalApi().readTimeout();
 *
 * Matching application.properties keys:
 *   app.default-page-size=10
 *   app.max-page-size=100
 *   app.cache.product-ttl=PT5M
 *   app.features.websocket-enabled=true
 *   app.external-api.connect-timeout=5000
 *   app.external-api.read-timeout=10000
 *   app.external-api.max-retries=3
 */
@ConfigMapping(prefix = "app")
public interface AppConfig {

    /** Default page size for paginated endpoints. */
    @WithName("default-page-size")
    @WithDefault("10")
    int defaultPageSize();

    /** Hard cap on page size to prevent large result sets. */
    @WithName("max-page-size")
    @WithDefault("100")
    int maxPageSize();

    /** Cache configuration group. */
    Cache cache();

    /** Feature flags configuration group. */
    Features features();

    /** External API configuration group. */
    @WithName("external-api")
    ExternalApi externalApi();

    // -------------------------------------------------------------------------
    // Nested config groups
    // -------------------------------------------------------------------------

    interface Cache {
        /** TTL for the products cache (ISO-8601 duration, e.g. PT5M = 5 minutes). */
        @WithName("product-ttl")
        @WithDefault("PT5M")
        String productTtl();
    }

    interface Features {
        /** Enable WebSocket notification endpoint. */
        @WithName("websocket-enabled")
        @WithDefault("true")
        boolean websocketEnabled();

        /** Enable Server-Sent Events streaming endpoint. */
        @WithName("sse-enabled")
        @WithDefault("true")
        boolean sseEnabled();

        /** Enable multipart file upload endpoint. */
        @WithName("file-upload-enabled")
        @WithDefault("true")
        boolean fileUploadEnabled();
    }

    interface ExternalApi {
        /** Connection timeout in milliseconds. */
        @WithName("connect-timeout")
        @WithDefault("5000")
        long connectTimeout();

        /** Read timeout in milliseconds. */
        @WithName("read-timeout")
        @WithDefault("10000")
        long readTimeout();

        /** Number of retry attempts for failed external API calls. */
        @WithName("max-retries")
        @WithDefault("3")
        int maxRetries();
    }
}
