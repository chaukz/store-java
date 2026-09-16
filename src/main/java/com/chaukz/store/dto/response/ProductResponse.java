package com.chaukz.store.dto.response;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        String sku,
        String brand,
        Boolean active,
        LocalDateTime createdAt
) {
}