package com.chaukz.store.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productVariantId,
        Long productId,
        String productName,
        String size,
        String color,
        BigDecimal price,
        Integer quantity,
        BigDecimal lineTotal
) {
}
