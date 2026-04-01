package com.template.quarkus.service;

import com.template.quarkus.dto.PagedResponse;
import com.template.quarkus.dto.ProductRequest;
import com.template.quarkus.dto.ProductResponse;
import com.template.quarkus.entity.Product;
import com.template.quarkus.exception.DuplicateSkuException;
import com.template.quarkus.exception.ProductNotFoundException;
import com.template.quarkus.mapper.ProductMapper;
import com.template.quarkus.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductMapper productMapper;

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    public PagedResponse<ProductResponse> getAll(int page, int size) {
        LOG.debugf("Fetching products — page=%d, size=%d", page, size);
        List<Product> products = productRepository.findPaginated(page, size);
        long total = productRepository.countActive();
        return PagedResponse.of(productMapper.toResponseList(products), page, size, total);
    }

    public ProductResponse getById(Long id) {
        LOG.debugf("Fetching product by id=%d", id);
        Product product = productRepository.findByIdOptional(id)
                .filter(p -> p.active)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> getByCategory(String category) {
        LOG.debugf("Fetching products by category=%s", category);
        return productMapper.toResponseList(productRepository.findByCategory(category));
    }

    public List<ProductResponse> search(String keyword) {
        LOG.debugf("Searching products by keyword=%s", keyword);
        return productMapper.toResponseList(productRepository.searchByName(keyword));
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Transactional
    public ProductResponse create(ProductRequest request) {
        LOG.debugf("Creating product with SKU=%s", request.sku);

        if (productRepository.existsBySku(request.sku)) {
            throw new DuplicateSkuException(request.sku);
        }

        Product product = productMapper.toEntity(request);
        productRepository.persist(product);
        LOG.infof("Created product id=%d, sku=%s", product.id, product.sku);
        return productMapper.toResponse(product);
    }

    @Transactional
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
        return productMapper.toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        LOG.debugf("Soft-deleting product id=%d", id);

        Product product = productRepository.findByIdOptional(id)
                .filter(p -> p.active)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Soft delete — keeps the record but marks it inactive
        product.active = false;
        productRepository.persist(product);
        LOG.infof("Soft-deleted product id=%d", id);
    }
}
