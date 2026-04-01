package com.template.quarkus.repository;

import com.template.quarkus.entity.Product;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Optional<Product> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }

    public List<Product> findByCategory(String category) {
        return list("category = ?1 and active = true", Sort.by("name"), category);
    }

    public List<Product> findActive() {
        return list("active = true", Sort.by("name"));
    }

    public List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return list("price >= ?1 and price <= ?2 and active = true",
                Sort.by("price"), minPrice, maxPrice);
    }

    public List<Product> findPaginated(int page, int size) {
        return findActive()
                .stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    public List<Product> searchByName(String keyword) {
        return list("lower(name) like ?1 and active = true",
                Sort.by("name"), "%" + keyword.toLowerCase() + "%");
    }

    public long countActive() {
        return count("active = true");
    }

    public boolean existsBySku(String sku) {
        return count("sku = ?1", sku) > 0;
    }

    public boolean existsBySkuAndIdNot(String sku, Long id) {
        return count("sku = ?1 and id != ?2", sku, id) > 0;
    }
}
