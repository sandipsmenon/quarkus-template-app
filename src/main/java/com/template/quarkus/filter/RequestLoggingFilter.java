package com.template.quarkus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.Instant;

/**
 * FEATURE: JAX-RS Filters — Request and response logging filter.
 *
 * JAX-RS filters run on EVERY request/response that goes through the JAX-RS layer.
 * They are different from CDI interceptors:
 *   - JAX-RS filters: HTTP-level — see headers, URIs, query params, status codes
 *   - CDI interceptors: method-level — see Java method arguments and return values
 *
 * Two filter types:
 *   ContainerRequestFilter   — runs BEFORE the resource method (pre-processing)
 *   ContainerResponseFilter  — runs AFTER the resource method (post-processing)
 *
 * Common uses:
 *   - Logging (this example)
 *   - Request ID injection for distributed tracing
 *   - Rate limiting
 *   - IP allow/deny lists
 *   - Custom authentication header parsing
 *   - Response header injection (HSTS, CSP, etc.)
 *
 * @Provider — tells JAX-RS to auto-discover and register this class.
 *
 * To restrict to specific endpoints only, add @NameBinding:
 *   1. Create a custom annotation: @NameBinding @interface LoggedRequest {}
 *   2. Add @LoggedRequest here and on the target resource method
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);

    // Property key used to pass start-time from request filter to response filter
    private static final String START_TIME_PROPERTY = "requestStartTime";

    // -------------------------------------------------------------------------
    // Pre-request: log incoming request details
    // -------------------------------------------------------------------------
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime());

        LOG.debugf("[FILTER] → %s %s | headers: %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                requestContext.getHeaders().size());
    }

    // -------------------------------------------------------------------------
    // Post-response: log outgoing response with timing
    // -------------------------------------------------------------------------
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        Long startNs = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        long elapsedMs = startNs != null
                ? (System.nanoTime() - startNs) / 1_000_000
                : -1;

        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().getPath();

        // Use WARN for 4xx/5xx to make them easy to spot in logs
        if (status >= 500) {
            LOG.warnf("[FILTER] ← %d %s %s (%dms) [SERVER ERROR]", status, method, uri, elapsedMs);
        } else if (status >= 400) {
            LOG.warnf("[FILTER] ← %d %s %s (%dms) [CLIENT ERROR]", status, method, uri, elapsedMs);
        } else {
            LOG.debugf("[FILTER] ← %d %s %s (%dms)", status, method, uri, elapsedMs);
        }
    }
}
