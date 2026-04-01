package com.template.quarkus.service;

import com.template.quarkus.dto.ProductRequest;
import com.template.quarkus.dto.ProductResponse;
import com.template.quarkus.entity.Product;
import com.template.quarkus.exception.DuplicateSkuException;
import com.template.quarkus.exception.ProductNotFoundException;
import com.template.quarkus.mapper.ProductMapper;
import com.template.quarkus.repository.ProductRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductServiceTest {

    @Inject
    ProductService productService;

    @InjectMock
    ProductRepository productRepository;

    @Inject
    ProductMapper productMapper;

    private Product sampleProduct;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.id = 1L;
        sampleProduct.name = "Test Product";
        sampleProduct.sku = "SKU-001";
        sampleProduct.price = BigDecimal.valueOf(49.99);
        sampleProduct.stockQuantity = 100;
        sampleProduct.category = "Electronics";
        sampleProduct.active = true;

        sampleRequest = new ProductRequest();
        sampleRequest.name = "Test Product";
        sampleRequest.sku = "SKU-001";
        sampleRequest.price = BigDecimal.valueOf(49.99);
        sampleRequest.stockQuantity = 100;
        sampleRequest.category = "Electronics";
        sampleRequest.active = true;
    }

    @Test
    void getById_existingActiveProduct_returnsResponse() {
        when(productRepository.findByIdOptional(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id).isEqualTo(1L);
        assertThat(response.name).isEqualTo("Test Product");
        assertThat(response.sku).isEqualTo("SKU-001");
    }

    @Test
    void getById_notFound_throwsProductNotFoundException() {
        when(productRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getById_inactiveProduct_throwsProductNotFoundException() {
        sampleProduct.active = false;
        when(productRepository.findByIdOptional(1L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getByCategory_returnsMatchingProducts() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(sampleProduct));

        List<ProductResponse> responses = productService.getByCategory("Electronics");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).category).isEqualTo("Electronics");
    }

    @Test
    void search_returnsMatchingProducts() {
        when(productRepository.searchByName("test")).thenReturn(List.of(sampleProduct));

        List<ProductResponse> responses = productService.search("test");

        assertThat(responses).hasSize(1);
    }

    @Test
    void create_duplicateSku_throwsDuplicateSkuException() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(sampleRequest))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-001");

        verify(productRepository, never()).persist(any(Product.class));
    }

    @Test
    void update_notFound_throwsProductNotFoundException() {
        when(productRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, sampleRequest))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_skuConflict_throwsDuplicateSkuException() {
        sampleProduct.sku = "SKU-ORIGINAL";
        when(productRepository.findByIdOptional(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsBySkuAndIdNot("SKU-001", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(1L, sampleRequest))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-001");
    }

    @Test
    void delete_notFound_throwsProductNotFoundException() {
        when(productRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
