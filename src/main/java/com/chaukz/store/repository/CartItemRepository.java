package com.chaukz.store.repository;

import com.chaukz.store.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    // Same fix as OrderItemRepository - CartMapper reads
    // variant.getProduct().getName(), so without this join, a cart with
    // N items means N extra queries for variants plus N more for
    // their products.
    @Query("SELECT ci FROM CartItem ci " +
            "JOIN FETCH ci.productVariant pv " +
            "JOIN FETCH pv.product " +
            "WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartIdWithVariantAndProduct(@Param("cartId") Long cartId);
}