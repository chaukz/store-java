package com.chaukz.store.service;

import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.response.PageResponse;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.ProductMapper;
import com.chaukz.store.model.Product;
import com.chaukz.store.repository.ProductRepository;
import com.chaukz.store.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    /**
     * Single entry point for browsing products. Every filter is optional -
     * pass only categoryId for "products in this category", only minPrice/
     * maxPrice for "products in this price range", any combination, or
     * nothing at all for "everything, paginated".
     */
    public PageResponse<ProductResponse> getAll(Long categoryId,
                                                BigDecimal minPrice,
                                                BigDecimal maxPrice,
                                                Boolean inStockOnly,
                                                String nameContains,
                                                Pageable pageable) {
        Specification<Product> spec = ProductSpecifications.withFilters(
                categoryId, minPrice, maxPrice, inStockOnly, nameContains);

        Page<Product> products = productRepository.findAll(spec, pageable);
        return PageResponse.from(products.map(productMapper::toResponse));
    }

    public ProductResponse getById(Long id) {
        Product product = findProductOrThrow(id);
        return productMapper.toResponse(product);
    }

    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);
        productMapper.updateEntity(product, request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    public void delete(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
