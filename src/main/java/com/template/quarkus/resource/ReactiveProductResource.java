package com.template.quarkus.resource;

import com.template.quarkus.dto.PagedResponse;
import com.template.quarkus.dto.ProductResponse;
import com.template.quarkus.service.ProductService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;

/**
 * FEATURE: Reactive Programming with Mutiny (Uni / Multi).
 *
 * Quarkus is built on a reactive engine (Vert.x). The Mutiny library
 * provides the reactive programming API with two core types:
 *
 *   Uni<T>   — represents a single asynchronous result (0 or 1 item).
 *              Like CompletableFuture but more composable.
 *              Use for: DB queries, HTTP calls, cache lookups.
 *
 *   Multi<T> — represents a stream of asynchronous items (0 to N items).
 *              Like RxJava Observable / Reactor Flux.
 *              Use for: streaming results, SSE, WebSocket messages.
 *
 * Why reactive?
 *   Traditional blocking code ties up a thread while waiting for I/O.
 *   Reactive code releases the thread during I/O, allowing it to serve
 *   other requests. This means far fewer threads handle far more load.
 *
 * Key Uni operations:
 *   Uni.createFrom().item(value)          — wrap a value
 *   Uni.createFrom().failure(exception)   — wrap an error
 *   uni.map(fn)                           — transform the value
 *   uni.flatMap(fn)                       — chain another async op
 *   uni.onFailure().recoverWithItem(val)  — error recovery
 *   uni.onItem().invoke(fn)               — side-effect without changing value
 *   uni.ifNoItem().after(duration).fail() — timeout
 *
 * Key Multi operations:
 *   Multi.createFrom().items(a, b, c)     — create from values
 *   Multi.createFrom().iterable(list)     — create from collection
 *   Multi.createFrom().ticks()            — time-based ticker
 *   multi.select().first(n)              — take first N items
 *   multi.filter(predicate)              — filter items
 *   multi.map(fn)                        — transform items
 *   multi.collect().asList()             — gather into Uni<List<T>>
 *
 * In Quarkus REST (RESTEasy Reactive), resource methods can return:
 *   - Uni<Response> or Uni<T>   → handled natively (no thread blocking)
 *   - Multi<T>                   → streamed to client (good for SSE/chunked)
 *   - T directly                → runs on worker thread (blocking allowed)
 */
@Path("/api/reactive/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reactive Products", description = "Demonstrates reactive programming with Mutiny Uni/Multi")
public class ReactiveProductResource {

    private static final Logger LOG = Logger.getLogger(ReactiveProductResource.class);

    @Inject
    ProductService productService;

    // -------------------------------------------------------------------------
    // Uni<Response> — single async result
    // The method returns immediately; the response is sent when the Uni resolves.
    // -------------------------------------------------------------------------
    @GET
    @Operation(
        summary = "Get all products reactively (Uni)",
        description = """
            Returns a Uni<Response> — a single async result.
            The JAX-RS layer subscribes to the Uni and sends the response
            when the value is available, without blocking a thread.

            This example wraps a blocking service call in Uni.createFrom().item()
            to demonstrate the API. In a truly reactive stack, the underlying
            DB call would also be non-blocking (e.g., Hibernate Reactive + Panache Reactive).
            """
    )
    public Uni<Response> getAllReactive(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {

        return Uni.createFrom()
                .item(() -> productService.getAll(page, size))    // wrap blocking call
                .map(result -> Response.ok(result).build())        // transform to Response
                .onFailure().recoverWithItem(e -> {                // graceful error handling
                    LOG.errorf(e, "Reactive getAll failed");
                    return Response.serverError()
                            .entity("Failed to fetch products: " + e.getMessage())
                            .build();
                });
    }

    // -------------------------------------------------------------------------
    // Uni with timeout — fail fast if the operation takes too long
    // -------------------------------------------------------------------------
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Get a product by ID reactively (Uni with timeout)",
        description = """
            Demonstrates Uni timeout: if the result isn't available within 2 seconds,
            the Uni fails with a TimeoutException and we return 504 Gateway Timeout.
            """
    )
    public Uni<Response> getByIdReactive(@PathParam("id") Long id) {

        return Uni.createFrom()
                .item(() -> productService.getById(id))
                .ifNoItem()
                    .after(Duration.ofSeconds(2))               // timeout after 2s
                    .failWith(new TimeoutException("Product lookup timed out"))
                .map(product -> Response.ok(product).build())
                .onFailure(TimeoutException.class)
                    .recoverWithItem(Response.status(504).entity("Request timed out").build())
                .onFailure()
                    .recoverWithItem(e -> Response.status(404)
                            .entity("Product not found: " + id).build());
    }

    // -------------------------------------------------------------------------
    // Multi — stream a collection of items
    // Each item is emitted individually; useful for large datasets or SSE.
    // -------------------------------------------------------------------------
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(
        summary = "Stream products as Server-Sent Events (Multi)",
        description = """
            Returns a Multi<ProductResponse> that emits each product as an SSE event.
            The client receives items one-by-one as they are emitted.

            Connect with EventSource in the browser:
              const es = new EventSource('/api/reactive/products/stream');
              es.onmessage = e => console.log(JSON.parse(e.data));
            """
    )
    public Multi<ProductResponse> streamAllProducts() {
        // Fetch all products (blocking), then emit them one-by-one as a stream
        List<ProductResponse> products = productService.getAll(0, 100).content();

        return Multi.createFrom()
                .iterable(products)
                .onItem().invoke(p -> LOG.debugf("[REACTIVE] Streaming product id=%d", p.id));
    }

    // -------------------------------------------------------------------------
    // Multi with ticks — emit heartbeat events on a schedule
    // -------------------------------------------------------------------------
    @GET
    @Path("/heartbeat")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(
        summary = "Heartbeat SSE stream (Multi with ticks)",
        description = """
            Emits a heartbeat message every 5 seconds.
            Demonstrates time-based Multi using ticks().every(Duration).

            Automatically stops after 12 ticks (1 minute) via .select().first(12).
            """
    )
    public Multi<String> heartbeat() {
        return Multi.createFrom()
                .ticks()
                .every(Duration.ofSeconds(5))     // emit every 5 seconds
                .select().first(12)               // stop after 12 ticks (1 minute)
                .map(tick -> String.format(
                    "{\"tick\":%d,\"message\":\"heartbeat\",\"timestamp\":\"%s\"}",
                    tick, java.time.Instant.now()
                ));
    }

    // -------------------------------------------------------------------------
    // Uni chaining — demonstrate flatMap for sequential async operations
    // -------------------------------------------------------------------------
    @GET
    @Path("/category/{category}/first")
    @Operation(
        summary = "Get first product in category with Uni chaining (flatMap)",
        description = """
            Demonstrates Uni chaining with flatMap:
            1. Get all products in a category (Uni<List<ProductResponse>>)
            2. Extract the first one (Uni<ProductResponse>)
            3. Map to a Response

            flatMap is used when the next step itself returns a Uni.
            map is used when the next step is a plain synchronous function.
            """
    )
    public Uni<Response> getFirstInCategory(@PathParam("category") String category) {

        return Uni.createFrom()
                .item(() -> productService.getByCategory(category))  // step 1: fetch list
                .flatMap(products -> {                                 // step 2: chain
                    if (products.isEmpty()) {
                        return Uni.createFrom().item(
                            Response.status(404)
                                .entity("No products in category: " + category)
                                .build()
                        );
                    }
                    return Uni.createFrom().item(
                        Response.ok(products.get(0)).build()
                    );
                });
    }

    // Simple custom exception for timeout demonstration
    private static class TimeoutException extends RuntimeException {
        TimeoutException(String message) { super(message); }
    }
}
