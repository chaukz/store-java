package com.chaukz.store.mapper;

import com.chaukz.store.dto.request.ProductVariantRequest;
import com.chaukz.store.dto.response.ProductVariantResponse;
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import org.springframework.stereotype.Component;

@Component
public class ProductVariantMapper {

    public ProductVariant toEntity(Product product, ProductVariantRequest request) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyRequest(variant, request);
        return variant;
    }

    public void updateEntity(ProductVariant variant, ProductVariantRequest request) {
        applyRequest(variant, request);
    }

    private void applyRequest(ProductVariant variant, ProductVariantRequest request) {
        variant.setSize(request.size());
        variant.setColor(request.color());
        variant.setPrice(request.price());
        variant.setStockQuantity(request.stockQuantity());
    }

    public ProductVariantResponse toResponse(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getProduct() != null ? variant.getProduct().getName() : null,
                variant.getSize(),
                variant.getColor(),
                variant.getPrice(),
                variant.getStockQuantity()
        );
    }
}