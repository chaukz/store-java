package com.chaukz.store.controller;

import com.chaukz.store.dto.request.CheckoutRequest;
import com.chaukz.store.dto.response.OrderResponse;
import com.chaukz.store.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        OrderResponse created = orderService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/orders")
    public List<OrderResponse> getMyOrders() {
        return orderService.getMyOrders();
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderResponse getById(@PathVariable Long orderId) {
        return orderService.getById(orderId);
    }

    @PostMapping("/api/orders/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        return orderService.cancel(orderId);
    }

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }
}
