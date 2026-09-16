package com.chaukz.store.repository;

import com.chaukz.store.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // ProductMapper reads category.getName(), not just its id - without
    // this, browsing a single page of products means one extra query
    // per product just to find out what category it's in. This is the
    // storefront's main browse endpoint, so it's the highest-traffic
    // fix in this whole batch.
    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}