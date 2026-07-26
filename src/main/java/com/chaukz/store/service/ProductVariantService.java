package com.chaukz.store.service;

import com.chaukz.store.dto.request.ProductVariantRequest;
import com.chaukz.store.dto.response.ProductVariantResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.ProductVariantMapper;
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import com.chaukz.store.repository.ProductRepository;
import com.chaukz.store.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper productVariantMapper;

    public ProductVariantService(ProductVariantRepository productVariantRepository,
                                 ProductRepository productRepository,
                                 ProductVariantMapper productVariantMapper) {
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
        this.productVariantMapper = productVariantMapper;
    }

    public List<ProductVariantResponse> getByProductId(Long productId) {
        return productVariantRepository.findByProductId(productId)
                .stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    public ProductVariantResponse getById(Long id) {
        ProductVariant variant = findVariantOrThrow(id);
        return productVariantMapper.toResponse(variant);
    }

    public ProductVariantResponse create(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductVariant variant = productVariantMapper.toEntity(product, request);
        ProductVariant saved = productVariantRepository.save(variant);
        return productVariantMapper.toResponse(saved);
    }

    public ProductVariantResponse update(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = findVariantOrThrow(variantId);
        productVariantMapper.updateEntity(variant, request);
        ProductVariant saved = productVariantRepository.save(variant);
        return productVariantMapper.toResponse(saved);
    }

    public void delete(Long variantId) {
        ProductVariant variant = findVariantOrThrow(variantId);
        productVariantRepository.delete(variant);
    }

    private ProductVariant findVariantOrThrow(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + id));
    }
}