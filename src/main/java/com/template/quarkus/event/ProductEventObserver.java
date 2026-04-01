package com.template.quarkus.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * FEATURE: CDI Events — Observer that reacts to ProductEvent lifecycle events.
 *
 * Any CDI bean can observe events simply by declaring a method with @Observes.
 * No registration, no configuration — Quarkus discovers observers at build time.
 *
 * Two observation styles:
 *
 *   @Observes       — synchronous: observer runs in the same thread as the producer,
 *                     BEFORE the fire() call returns. Great for auditing, validation.
 *
 *   @ObservesAsync  — asynchronous: observer runs on a worker thread pool,
 *                     fire() returns immediately. Great for emails, notifications,
 *                     analytics, or any non-critical side-effects.
 *
 * Multiple observers for the same event are all invoked.
 * Observer execution order can be controlled with @Priority.
 *
 * In production, replace the log statements with:
 *   - Audit trail persistence (save to audit_log table)
 *   - Push notifications via WebSocket (see ProductNotificationSocket)
 *   - Email/Slack alerts
 *   - Analytics event tracking
 */
@ApplicationScoped
public class ProductEventObserver {

    private static final Logger LOG = Logger.getLogger(ProductEventObserver.class);

    // -------------------------------------------------------------------------
    // Synchronous observer — runs in the caller's thread before fire() returns.
    // Use this for things that MUST complete before the caller continues
    // (e.g., writing an audit record in the same transaction).
    // -------------------------------------------------------------------------
    void onProductEventSync(@Observes ProductEvent event) {
        LOG.infof("[CDI-EVENT] Synchronous observer received: %s", event);

        switch (event.getType()) {
            case CREATED ->
                LOG.infof("[CDI-EVENT] Product %d was CREATED (details: %s)",
                    event.getProductId(), event.getDetails());
            case UPDATED ->
                LOG.infof("[CDI-EVENT] Product %d was UPDATED", event.getProductId());
            case DELETED ->
                LOG.infof("[CDI-EVENT] Product %d was DELETED", event.getProductId());
        }
    }

    // -------------------------------------------------------------------------
    // Asynchronous observer — runs on a separate thread; fire() returns first.
    // Use this for non-critical side-effects like notifications or analytics.
    // -------------------------------------------------------------------------
    void onProductEventAsync(@ObservesAsync ProductEvent event) {
        LOG.debugf("[CDI-EVENT] Async observer received: %s (thread: %s)",
                event, Thread.currentThread().getName());

        // Simulate sending a notification (e.g., push notification, email)
        // In production, inject a NotificationService here
        if (event.getType() == ProductEvent.Type.CREATED) {
            LOG.infof("[CDI-EVENT] [ASYNC] Sending 'new product available' notification for product %d",
                    event.getProductId());
        }
    }
}
