package com.chaukz.store.dto.response;

import com.chaukz.store.model.enums.OrderStatus;
import com.chaukz.store.model.enums.PaymentMethod;
import com.chaukz.store.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Long addressId,
        String shippingAddress,
        OrderStatus orderStatus,
        BigDecimal total,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        Integer itemCount,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod
) {
}
