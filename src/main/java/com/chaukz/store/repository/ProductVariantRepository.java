package com.chaukz.store.repository;

import com.chaukz.store.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    // Listing variants for ONE known product would otherwise fetch that
    // same product row over and over - once per variant - since
    // ProductVariantMapper reads product.getName(). Five variants of
    // one product previously meant five identical extra queries for
    // data that never changes within the request.
    @Query("SELECT pv FROM ProductVariant pv " +
            "JOIN FETCH pv.product " +
            "WHERE pv.product.id = :productId")
    List<ProductVariant> findByProductIdWithProduct(@Param("productId") Long productId);
}