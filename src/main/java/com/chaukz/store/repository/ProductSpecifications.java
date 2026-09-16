package com.chaukz.store.repository;

import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(Long categoryId,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       Boolean inStockOnly,
                                                       String nameContains) {

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ProductVariant> v = sub.from(ProductVariant.class);
                sub.select(v.get("id")).where(cb.and(
                        cb.equal(v.get("product").get("id"), root.get("id")),
                        cb.greaterThanOrEqualTo(v.get("price"), minPrice)));
                return cb.exists(sub);
            });
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ProductVariant> v = sub.from(ProductVariant.class);
                sub.select(v.get("id")).where(cb.and(
                        cb.equal(v.get("product").get("id"), root.get("id")),
                        cb.lessThanOrEqualTo(v.get("price"), maxPrice)));
                return cb.exists(sub);
            });
        }

        if (Boolean.TRUE.equals(inStockOnly)) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ProductVariant> v = sub.from(ProductVariant.class);
                sub.select(v.get("id")).where(cb.and(
                        cb.equal(v.get("product").get("id"), root.get("id")),
                        cb.greaterThan(v.get("stockQuantity"), 0)));
                return cb.exists(sub);
            });
        }

        if (nameContains != null && !nameContains.isBlank()) {
            String pattern = "%" + nameContains.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern));
        }

        return spec;
    }
}
