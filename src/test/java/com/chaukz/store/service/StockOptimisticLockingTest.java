package com.chaukz.store.service;

import com.chaukz.store.model.Category;
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import com.chaukz.store.repository.CategoryRepository;
import com.chaukz.store.repository.ProductRepository;
import com.chaukz.store.repository.ProductVariantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the checkout race-condition fix: two "requests" that both read
 * the same stock level, then both try to decrement it, must not both
 * succeed.
 *
 * This deliberately does NOT spin up real threads - it doesn't need to.
 * Reading the same row via two separate, unwrapped repository calls
 * already produces two independent Java copies of the same starting
 * state, because each Spring Data call outside a surrounding
 * @Transactional opens and closes its own transaction. That's exactly
 * what two simultaneous HTTP checkout requests would also see - so this
 * test exercises the same underlying mechanism, deterministically,
 * without any flakiness from real thread scheduling.
 */
@SpringBootTest
class StockOptimisticLockingTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;

    private Long variantId;
    private Long productId;
    private Long categoryId;

    private void seedVariantWithStock(int stock) {
        Category category = new Category();
        category.setName("Test category " + System.nanoTime());
        Category savedCategory = categoryRepository.save(category);
        categoryId = savedCategory.getId();

        Product product = new Product();
        product.setCategory(savedCategory);
        product.setName("Test product");
        product.setActive(true);
        product.setCreatedAt(java.time.LocalDateTime.now());
        Product savedProduct = productRepository.save(product);
        productId = savedProduct.getId();

        ProductVariant variant = new ProductVariant();
        variant.setProduct(savedProduct);
        variant.setSize("One size");
        variant.setPrice(new BigDecimal("100.00"));
        variant.setStockQuantity(stock);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        variantId = savedVariant.getId();
    }

    @AfterEach
    void cleanUp() {
        if (variantId != null) {
            productVariantRepository.deleteById(variantId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (categoryId != null) {
            categoryRepository.deleteById(categoryId);
        }
    }

    @Test
    void secondWriterWithStaleVersionIsRejected_notSilentlyOverwritten() {
        seedVariantWithStock(1);

        // Two independent reads of the same row - both see version 0,
        // stockQuantity 1. This is exactly what two simultaneous
        // checkout requests for the last unit in stock would each see.
        ProductVariant readByFirstRequest = productVariantRepository.findById(variantId).orElseThrow();
        ProductVariant readBySecondRequest = productVariantRepository.findById(variantId).orElseThrow();

        // First request "wins the race": decrements stock, saves,
        // commits. The version stored in the database is now 1.
        readByFirstRequest.setStockQuantity(readByFirstRequest.getStockQuantity() - 1);
        productVariantRepository.saveAndFlush(readByFirstRequest);

        // Second request is still working from version 0. Without
        // optimistic locking this would silently overwrite the first
        // request's update and both "sales" would succeed on one unit
        // of stock. With it, Hibernate's UPDATE ... WHERE version = 0
        // matches zero rows, and Spring translates that into this
        // exception instead of pretending nothing happened.
        readBySecondRequest.setStockQuantity(readBySecondRequest.getStockQuantity() - 1);
        assertThrows(OptimisticLockingFailureException.class,
                () -> productVariantRepository.saveAndFlush(readBySecondRequest));

        // Stock reflects exactly one successful sale - never went
        // negative, never oversold.
        ProductVariant finalState = productVariantRepository.findById(variantId).orElseThrow();
        assertThat(finalState.getStockQuantity()).isEqualTo(0);
    }
}