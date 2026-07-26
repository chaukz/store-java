package com.chaukz.store.controller;

import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Public endpoints

    @GetMapping("/api/products")
    public List<ProductResponse> getAll(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return productService.getByCategoryId(categoryId);
        }
        return productService.getAll();
    }

    @GetMapping("/api/products/search")
    public List<ProductResponse> search(@RequestParam String query) {
        return productService.search(query);
    }

    @GetMapping("/api/products/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    // Admin endpoints

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