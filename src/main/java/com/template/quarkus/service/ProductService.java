package com.template.quarkus.service;

import com.template.quarkus.dto.PagedResponse;
import com.template.quarkus.dto.ProductRequest;
import com.template.quarkus.dto.ProductResponse;
import com.template.quarkus.entity.Product;
import com.template.quarkus.event.ProductEvent;
import com.template.quarkus.exception.DuplicateSkuException;
import com.template.quarkus.exception.ProductNotFoundException;
import com.template.quarkus.interceptor.Logged;
import com.template.quarkus.interceptor.Timed;
import com.template.quarkus.mapper.ProductMapper;
import com.template.quarkus.repository.ProductRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Product business logic — enhanced with:
 *
 * FEATURE: Caching (@CacheResult / @CacheInvalidate)
 *   @CacheResult(cacheName = "products")
 *     — On the first call, executes the method and stores the result in the cache.
 *     — On subsequent calls with the same cache key, returns the cached result
 *       WITHOUT executing the method body.
 *     — Cache key is derived from the method parameters by default.
 *     — TTL and max size are configured in application.properties:
 *         quarkus.cache.caffeine."products".expire-after-write=PT5M
 *
 *   @CacheInvalidate(cacheName = "products")
 *     — Removes the cached entry matching the method's cache key.
 *     — Use on write methods (create, update, delete) to keep cache consistent.
 *
 *   @CacheInvalidateAll(cacheName = "products")
 *     — Removes ALL entries from the named cache.
 *     — Useful when a write affects multiple cached results.
 *
 * FEATURE: CDI Events (fire-and-forget notifications)
 *   @Inject Event<ProductEvent> productEvents;
 *   productEvents.fire(ProductEvent.created(...))     — synchronous
 *   productEvents.fireAsync(ProductEvent.created(...)) — async (non-blocking)
 *
 * FEATURE: CDI Interceptors (@Logged, @Timed)
 *   @Logged — LoggingInterceptor logs method entry/exit/exception automatically
 *   @Timed  — TimedInterceptor records execution duration automatically
 */
@ApplicationScoped
@Logged   // Apply LoggingInterceptor to ALL methods in this class
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductMapper productMapper;

    /**
     * CDI Event bus for product lifecycle events.
     * Inject Event<T> and call fire() to notify all observers.
     */
    @Inject
    Event<ProductEvent> productEvents;

    // =========================================================================
    // Read operations
    // =========================================================================

    /**
     * @CacheResult: results are cached by (page, size) key.
     * Cache name "products" — TTL and max-size configured in application.properties.
     */
    @CacheResult(cacheName = "products")
    public PagedResponse<ProductResponse> getAll(int page, int size) {
        LOG.debugf("Fetching products — page=%d, size=%d", page, size);
        List<Product> products = productRepository.findPaginated(page, size);
        long total = productRepository.countActive();
        return PagedResponse.of(productMapper.toResponseList(products), page, size, total);
    }

    /**
     * @CacheResult: result cached by product ID.
     * If the product is requested again within the TTL, no DB query is made.
     */
    @CacheResult(cacheName = "products")
    public ProductResponse getById(Long id) {
        LOG.debugf("Fetching product by id=%d", id);
        Product product = productRepository.findByIdOptional(id)
                .filter(p -> p.active)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponse(product);
    }

    /**
     * @CacheResult: results cached by category name.
     * Cache name "categories" has a longer TTL (categories change less often).
     */
    @CacheResult(cacheName = "categories")
    public List<ProductResponse> getByCategory(String category) {
        LOG.debugf("Fetching products by category=%s", category);
        return productMapper.toResponseList(productRepository.findByCategory(category));
    }

    /** Search results are NOT cached — search queries are too varied to cache effectively. */
    public List<ProductResponse> search(String keyword) {
        LOG.debugf("Searching products by keyword=%s", keyword);
        return productMapper.toResponseList(productRepository.searchByName(keyword));
    }

    // =========================================================================
    // Write operations
    // =========================================================================

    /**
     * @CacheInvalidateAll: clears the entire "products" cache and "categories"
     * cache after a new product is created, ensuring stale data is evicted.
     *
     * Fires a CDI ProductEvent.created() after the product is persisted.
     * Observers (ProductEventObserver, ProductNotificationSocket) react asynchronously.
     */
    @Transactional
    @Timed   // Apply TimedInterceptor to this specific method
    @CacheInvalidateAll(cacheName = "products")
    @CacheInvalidateAll(cacheName = "categories")
    public ProductResponse create(ProductRequest request) {
        LOG.debugf("Creating product with SKU=%s", request.sku);

        if (productRepository.existsBySku(request.sku)) {
            throw new DuplicateSkuException(request.sku);
        }

        Product product = productMapper.toEntity(request);
        productRepository.persist(product);
        LOG.infof("Created product id=%d, sku=%s", product.id, product.sku);

        // Fire CDI event — all observers are notified
        // fireAsync() is non-blocking: this method returns without waiting for observers
        productEvents.fireAsync(ProductEvent.created(product.id, product.sku));

        return productMapper.toResponse(product);
    }

    /**
     * @CacheInvalidate: removes the cached entry for this specific product ID.
     * Also invalidates the categories cache since category assignment may change.
     *
     * Fires a CDI ProductEvent.updated() after the update is persisted.
     */
    @Transactional
    @Timed
    @CacheInvalidate(cacheName = "products")
    @CacheInvalidateAll(cacheName = "categories")
    public ProductResponse update(Long id, ProductRequest request) {
        LOG.debugf("Updating product id=%d", id);

        Product product = productRepository.findByIdOptional(id)
                .filter(p -> p.active)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.sku.equals(request.sku) && productRepository.existsBySkuAndIdNot(request.sku, id)) {
            throw new DuplicateSkuException(request.sku);
        }

        productMapper.updateEntityFromRequest(request, product);
        productRepository.persist(product);
        LOG.infof("Updated product id=%d", id);

        productEvents.fireAsync(ProductEvent.updated(id));

        return productMapper.toResponse(product);
    }

    /**
     * @CacheInvalidate: removes the cached entry for this product.
     * Fires a CDI ProductEvent.deleted() event after soft-deleting.
     */
    @Transactional
    @CacheInvalidate(cacheName = "products")
    @CacheInvalidateAll(cacheName = "categories")
    public void delete(Long id) {
        LOG.debugf("Soft-deleting product id=%d", id);

        Product product = productRepository.findByIdOptional(id)
                .filter(p -> p.active)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Soft delete — keeps the record but marks it inactive
        product.active = false;
        productRepository.persist(product);
        LOG.infof("Soft-deleted product id=%d", id);

        productEvents.fireAsync(ProductEvent.deleted(id));
    }
}
