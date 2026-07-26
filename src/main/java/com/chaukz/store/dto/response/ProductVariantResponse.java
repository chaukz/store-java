package com.chaukz.store.dto.response;

import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        Long productId,
        String productName,
        String size,
        String color,
        BigDecimal price,
        Integer stockQuantity
) {
}