package com.template.quarkus.event;

import java.time.Instant;

/**
 * FEATURE: CDI Events — Event payload for product lifecycle changes.
 *
 * CDI Events (javax.enterprise.event.Event) enable loose coupling:
 * a producer fires events without knowing who handles them, and
 * observers react independently without knowing who fired the event.
 *
 * This is the "Observer" pattern built into CDI.
 *
 * Flow:
 *   ProductService  →  Event<ProductEvent>.fire(...)  →  ProductEventObserver.onProductEvent(...)
 *
 * How to FIRE an event (in a CDI bean):
 *   @Inject Event<ProductEvent> productEvents;
 *   productEvents.fire(ProductEvent.created(productId));
 *
 * How to OBSERVE an event (in a CDI bean):
 *   void onEvent(@Observes ProductEvent event) { ... }
 *
 * Async events (fire-and-forget, non-blocking):
 *   productEvents.fireAsync(ProductEvent.created(productId));
 *
 * Qualifier-based filtering (only observe specific types):
 *   void onCreated(@Observes @Created ProductEvent event) { ... }
 */
public class ProductEvent {

    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Long productId;
    private final Type type;
    private final Instant occurredAt;
    private final String details;

    private ProductEvent(Long productId, Type type, String details) {
        this.productId = productId;
        this.type = type;
        this.details = details;
        this.occurredAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Factory methods — self-documenting event construction
    // -------------------------------------------------------------------------

    public static ProductEvent created(Long productId, String sku) {
        return new ProductEvent(productId, Type.CREATED, "SKU: " + sku);
    }

    public static ProductEvent updated(Long productId) {
        return new ProductEvent(productId, Type.UPDATED, null);
    }

    public static ProductEvent deleted(Long productId) {
        return new ProductEvent(productId, Type.DELETED, null);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Long getProductId()  { return productId; }
    public Type getType()       { return type; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getDetails()  { return details; }

    @Override
    public String toString() {
        return String.format("ProductEvent{type=%s, productId=%d, at=%s}", type, productId, occurredAt);
    }
}
