package com.chaukz.store.repository;

import com.chaukz.store.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Builds a Product query predicate from whichever filters are actually
 * present. Each filter is optional and independent - a request can supply
 * any combination (or none) and only the matching pieces get added to the
 * WHERE clause.
 */
public class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(Long categoryId,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       Boolean inStockOnly,
                                                       String nameContains) {

        // Start with "always true" - every subsequent filter narrows this down.
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        if (Boolean.TRUE.equals(inStockOnly)) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThan(root.get("stockQuantity"), 0));
        }

        if (nameContains != null && !nameContains.isBlank()) {
            String pattern = "%" + nameContains.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern));
        }

        return spec;
    }
}
