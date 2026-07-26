package com.chaukz.store.mapper;

import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.model.Category;
import com.chaukz.store.model.Product;
import com.chaukz.store.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProductMapper {

    private final CategoryRepository categoryRepository;

    public ProductMapper(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        product.setCreatedAt(LocalDateTime.now());
        return product;
    }

    public void updateEntity(Product product, ProductRequest request) {
        applyRequest(product, request);
    }

    private void applyRequest(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.categoryId()));

        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(request.active() != null ? request.active() : true);
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getBrand(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getActive(),
                product.getCreatedAt()
        );
    }
}