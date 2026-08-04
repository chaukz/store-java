package com.chaukz.store.dto.request;

import com.chaukz.store.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull(message = "Address id is required")
        Long addressId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {
}