package com.chaukz.store.service;

import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.model.Product;
import com.chaukz.store.repository.ProductRepository;
import org.springframework.stereotype.Service;
import com.chaukz.store.mapper.ProductMapper;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> getByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> search(String query) {
        return productRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(productMapper::toResponse)
                .toList();
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