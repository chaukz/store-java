package com.chaukz.store.controller;

import com.chaukz.store.dto.request.UpdateOrderStatusRequest;
import com.chaukz.store.dto.response.OrderResponse;
import com.chaukz.store.service.OrderStatusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminOrderController {

    private final OrderStatusService orderStatusService;

    public AdminOrderController(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @PutMapping("/api/admin/orders/{orderId}/status")
    public OrderResponse updateStatus(@PathVariable Long orderId,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderStatusService.updateStatus(orderId, request);
    }
}
