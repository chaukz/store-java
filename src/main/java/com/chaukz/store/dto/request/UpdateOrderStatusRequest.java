package com.chaukz.store.dto.request;

import com.chaukz.store.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Order status is required")
        OrderStatus orderStatus
) {
}
