package com.chaukz.store.controller;

import com.chaukz.store.dto.request.ProductVariantRequest;
import com.chaukz.store.dto.response.ProductVariantResponse;
import com.chaukz.store.service.ProductVariantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    // Public endpoint - browse variants for a product

    @GetMapping("/api/products/{productId}/variants")
    public List<ProductVariantResponse> getByProductId(@PathVariable Long productId) {
        return productVariantService.getByProductId(productId);
    }

    // Admin endpoints

    @PostMapping("/api/admin/products/{productId}/variants")
    public ResponseEntity<ProductVariantResponse> create(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse created = productVariantService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/variants/{variantId}")
    public ProductVariantResponse update(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        return productVariantService.update(variantId, request);
    }

    @DeleteMapping("/api/admin/variants/{variantId}")
    public ResponseEntity<Void> delete(@PathVariable Long variantId) {
        productVariantService.delete(variantId);
        return ResponseEntity.noContent().build();
    }
}