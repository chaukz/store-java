package com.chaukz.store.dto.request;

import com.chaukz.store.model.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(

        @NotNull(message = "Payment status is required")
        PaymentStatus paymentStatus,

        String transactionId
) {
}
