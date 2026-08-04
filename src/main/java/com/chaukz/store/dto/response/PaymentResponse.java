package com.chaukz.store.dto.response;

import com.chaukz.store.model.enums.PaymentMethod;
import com.chaukz.store.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String transactionId,
        LocalDateTime paidAt
) {
}
