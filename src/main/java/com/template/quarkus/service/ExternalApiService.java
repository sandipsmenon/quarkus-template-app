package com.template.quarkus.service;

import com.template.quarkus.client.JsonPlaceholderClient;
import com.template.quarkus.dto.ExternalPostDto;
import com.template.quarkus.dto.ExternalPostSummary;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * External API integration — enhanced with:
 *
 * FEATURE: Fault Tolerance (quarkus-smallrye-fault-tolerance / MicroProfile Fault Tolerance).
 *
 * Production systems must handle failures gracefully. The MicroProfile Fault Tolerance
 * spec provides four key patterns implemented as CDI interceptor annotations:
 *
 * ┌─────────────────┬───────────────────────────────────────────────────────────────┐
 * │ @Retry          │ Retry the call N times with optional delay between attempts.  │
 * │                 │ Good for transient network blips.                             │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ @Timeout        │ Fail fast if the call exceeds a time limit.                   │
 * │                 │ Prevents slow dependencies from blocking your threads.        │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ @CircuitBreaker │ After N failures, "open" the circuit and reject calls fast.   │
 * │                 │ After a wait window, "half-open" and try one probe call.      │
 * │                 │ Prevents overloading a struggling dependency.                 │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ @Fallback       │ If the method fails (after retries), call a fallback method   │
 * │                 │ or handler. Ensures the user always gets a response.          │
 * └─────────────────┴───────────────────────────────────────────────────────────────┘
 *
 * Execution order when all four are combined on the same method:
 *   Fallback( CircuitBreaker( Retry( Timeout( method() ) ) ) )
 *
 * Annotation composition:
 *   Retry wraps Timeout → each retry attempt has its own timeout.
 *   CircuitBreaker wraps Retry → too many retry-exhausted attempts open the circuit.
 *   Fallback wraps CircuitBreaker → if the circuit is open, fallback fires immediately.
 *
 * Config overrides (no recompile needed):
 *   ExternalApiService/getAllPostSummaries/Retry/maxRetries=5
 *   ExternalApiService/getAllPostSummaries/Timeout/value=3000
 *
 * FEATURE: Caching — external API responses are cached to reduce outbound calls.
 */
@ApplicationScoped
public class ExternalApiService {

    private static final Logger LOG = Logger.getLogger(ExternalApiService.class);

    @RestClient
    @Inject
    JsonPlaceholderClient jsonPlaceholderClient;

    // =========================================================================
    // getAllPostSummaries — full fault tolerance stack
    // =========================================================================

    /**
     * @Retry: retry up to 3 times on failure, waiting 500ms between attempts.
     *   maxRetries         — number of additional attempts after the first failure
     *   delay              — wait time between retries
     *   delayUnit          — unit for delay (MILLIS, SECONDS, etc.)
     *   retryOn            — which exception types trigger a retry (default: all)
     *   abortOn            — which exception types should NOT be retried
     *
     * @Timeout: fail with TimeoutException if the call takes more than 5 seconds.
     *   value    — the timeout duration
     *   unit     — ChronoUnit (MILLIS, SECONDS, MINUTES, etc.)
     *
     * @CircuitBreaker: track failure rate over a rolling window.
     *   requestVolumeThreshold — minimum requests in the window before computing failure rate
     *   failureRatio           — fraction of failures to open the circuit (0.5 = 50%)
     *   delay                  — how long the circuit stays open before trying again (ms)
     *   successThreshold       — how many consecutive successes close the circuit again
     *
     * @Fallback: if the circuit is open or all retries are exhausted, call fallback.
     *   fallbackMethod — name of a method in the same class with the same signature
     *
     * @CacheResult: cache successful results to avoid repeated external calls.
     */
    @Retry(maxRetries = 3, delay = 500, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
        requestVolumeThreshold = 4,
        failureRatio           = 0.5,
        delay                  = 10_000,
        successThreshold       = 2
    )
    @Fallback(fallbackMethod = "getAllPostSummariesFallback")
    @CacheResult(cacheName = "external-posts")
    public List<ExternalPostSummary> getAllPostSummaries() {
        LOG.debug("Fetching all posts from JSONPlaceholder");
        List<ExternalPostDto> posts = jsonPlaceholderClient.getAllPosts();
        return posts.stream()
                .map(ExternalPostSummary::from)
                .toList();
    }

    // =========================================================================
    // getPostSummaryById — retry + timeout + fallback
    // =========================================================================

    /**
     * @Retry with abortOn: don't retry 404 errors (the resource simply doesn't exist).
     * @Fallback: return a placeholder if the external API is unreachable.
     */
    @Retry(
        maxRetries = 2,
        delay      = 300,
        delayUnit  = ChronoUnit.MILLIS,
        abortOn    = WebApplicationException.class  // 4xx errors: don't retry
    )
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "getPostSummaryByIdFallback")
    public ExternalPostSummary getPostSummaryById(Integer id) {
        LOG.debugf("Fetching post id=%d from JSONPlaceholder", id);
        try {
            ExternalPostDto post = jsonPlaceholderClient.getPostById(id);
            return ExternalPostSummary.from(post);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new WebApplicationException("External post not found with ID: " + id, 404);
            }
            throw e;
        }
    }

    // =========================================================================
    // getPostSummariesByUser — retry + timeout + fallback
    // =========================================================================

    @Retry(maxRetries = 3, delay = 500, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10_000)
    @Fallback(fallbackMethod = "getPostSummariesByUserFallback")
    public List<ExternalPostSummary> getPostSummariesByUser(Integer userId) {
        LOG.debugf("Fetching posts for userId=%d from JSONPlaceholder", userId);
        List<ExternalPostDto> posts = jsonPlaceholderClient.getPostsByUser(userId);
        return posts.stream()
                .map(ExternalPostSummary::from)
                .toList();
    }

    // =========================================================================
    // Fallback methods — must have the same signature as the primary method
    // =========================================================================

    /**
     * Fallback for getAllPostSummaries().
     * Called when:
     *   - All retries are exhausted
     *   - The circuit breaker is open
     *   - Timeout is exceeded
     *
     * Returns a meaningful degraded response rather than an error.
     * In production, you might:
     *   - Return cached data from a local store
     *   - Return a "service temporarily unavailable" sentinel
     *   - Increment a counter for monitoring
     */
    List<ExternalPostSummary> getAllPostSummariesFallback() {
        LOG.warn("[FALLBACK] getAllPostSummaries — external API unavailable, returning empty list");
        return List.of();
    }

    ExternalPostSummary getPostSummaryByIdFallback(Integer id) {
        LOG.warnf("[FALLBACK] getPostSummaryById(%d) — external API unavailable", id);
        throw new WebApplicationException(
            "External API is temporarily unavailable. Please try again later.", 503);
    }

    List<ExternalPostSummary> getPostSummariesByUserFallback(Integer userId) {
        LOG.warnf("[FALLBACK] getPostSummariesByUser(%d) — external API unavailable, returning empty list", userId);
        return List.of();
    }
}
