package com.chaukz.store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        String sku,
        String brand,
        BigDecimal price,
        Integer stockQuantity,
        Boolean active,
        LocalDateTime createdAt
) {
}