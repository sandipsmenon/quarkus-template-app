package com.template.quarkus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.quarkus.event.ProductEvent;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FEATURE: WebSockets (quarkus-websockets-next) — Real-time product notifications.
 *
 * WebSockets provide a persistent, full-duplex communication channel between
 * client and server over a single TCP connection. Unlike HTTP (request/response),
 * the server can PUSH messages to the client at any time.
 *
 * Use cases:
 *   - Real-time notifications (new product added, price changed)
 *   - Live dashboards (stock levels, order status)
 *   - Collaborative features (multiple users editing the same resource)
 *   - Chat / messaging applications
 *
 * quarkus-websockets-next API (Quarkus 3.x):
 *   @WebSocket(path = "/ws/...") — declares a WebSocket endpoint
 *   @OnOpen    — called when a client connects
 *   @OnClose   — called when a client disconnects
 *   @OnTextMessage — called when a text message is received from a client
 *   @OnBinaryMessage — called for binary (byte[]) messages
 *   WebSocketConnection — injected per-connection to send messages back
 *
 * Connecting from a browser (JavaScript):
 *   const ws = new WebSocket("ws://localhost:8080/ws/products");
 *   ws.onmessage = (e) => console.log(JSON.parse(e.data));
 *   ws.send(JSON.stringify({ action: "subscribe", category: "Electronics" }));
 *
 * This endpoint listens to CDI events (ProductEvent) and broadcasts
 * notifications to all connected WebSocket clients.
 */
@WebSocket(path = "/ws/products")
@ApplicationScoped
public class ProductNotificationSocket {

    private static final Logger LOG = Logger.getLogger(ProductNotificationSocket.class);

    // Active connections — keyed by connection ID for thread-safe access
    private final Map<String, WebSocketConnection> connections = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Lifecycle: client connects
    // -------------------------------------------------------------------------
    @OnOpen
    public String onOpen(WebSocketConnection connection) {
        connections.put(connection.id(), connection);
        LOG.infof("[WEBSOCKET] Client connected: id=%s, total=%d", connection.id(), connections.size());
        return """
                {"type":"CONNECTED","message":"Subscribed to product notifications","connectionId":"%s"}
                """.formatted(connection.id()).strip();
    }

    // -------------------------------------------------------------------------
    // Lifecycle: client disconnects
    // -------------------------------------------------------------------------
    @OnClose
    public void onClose(WebSocketConnection connection) {
        connections.remove(connection.id());
        LOG.infof("[WEBSOCKET] Client disconnected: id=%s, remaining=%d", connection.id(), connections.size());
    }

    // -------------------------------------------------------------------------
    // Incoming message: clients can send commands (e.g., ping, subscribe filters)
    // -------------------------------------------------------------------------
    @OnTextMessage
    public String onMessage(String message, WebSocketConnection connection) {
        LOG.debugf("[WEBSOCKET] Message from %s: %s", connection.id(), message);
        // Echo back with server timestamp — extend this for real subscription logic
        return """
                {"type":"ACK","echo":%s,"connectionId":"%s"}
                """.formatted(message, connection.id()).strip();
    }

    // -------------------------------------------------------------------------
    // CDI Event observer: broadcast product change events to all connected clients
    // Uses @ObservesAsync so the HTTP request that triggered the event
    // is not blocked while we push to (potentially many) WebSocket clients.
    // -------------------------------------------------------------------------
    void onProductEvent(@ObservesAsync ProductEvent event) {
        if (connections.isEmpty()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "type", "PRODUCT_" + event.getType().name(),
                "productId", event.getProductId(),
                "occurredAt", event.getOccurredAt().toString(),
                "details", event.getDetails() != null ? event.getDetails() : ""
            ));

            LOG.debugf("[WEBSOCKET] Broadcasting %s event to %d client(s)",
                    event.getType(), connections.size());

            // Broadcast to all open connections
            connections.values().forEach(conn -> {
                if (conn.isOpen()) {
                    conn.sendTextAndAwait(payload);
                }
            });

        } catch (Exception e) {
            LOG.errorf(e, "[WEBSOCKET] Failed to broadcast event: %s", event);
        }
    }

    /** Returns the number of currently connected clients. */
    public int getConnectionCount() {
        return connections.size();
    }
}
