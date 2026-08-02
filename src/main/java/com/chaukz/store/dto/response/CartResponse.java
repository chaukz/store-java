package com.chaukz.store.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items,
        Integer itemCount,
        BigDecimal total
) {
}