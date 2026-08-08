package com.chaukz.store.controller;

import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.response.PageResponse;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Browse products",
            description = "All filters are optional and combinable. Omit any of them to widen the search.")
    @GetMapping("/api/products")
    public PageResponse<ProductResponse> getAll(
            @Parameter(description = "Filter to a single category") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Minimum price, inclusive") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price, inclusive") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Only show products with stock > 0") @RequestParam(required = false) Boolean inStock,
            @Parameter(description = "Case-insensitive partial match on product name") @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return productService.getAll(categoryId, minPrice, maxPrice, inStock, query, pageable);
    }

    @Operation(summary = "Search products by name (convenience wrapper around /api/products)")
    @GetMapping("/api/products/search")
    public PageResponse<ProductResponse> search(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return productService.getAll(null, null, null, null, query, pageable);
    }

    @GetMapping("/api/products/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping("/api/admin/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
