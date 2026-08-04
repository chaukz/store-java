package com.chaukz.store.controller;

import com.chaukz.store.dto.request.UpdatePaymentStatusRequest;
import com.chaukz.store.dto.response.PaymentResponse;
import com.chaukz.store.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/orders/{orderId}/payment")
    public PaymentResponse getByOrderId(@PathVariable Long orderId) {
        return paymentService.getByOrderId(orderId);
    }

    @GetMapping("/api/admin/payments")
    public List<PaymentResponse> getAll() {
        return paymentService.getAll();
    }

    @PutMapping("/api/admin/payments/{paymentId}/status")
    public PaymentResponse updateStatus(@PathVariable Long paymentId,
                                        @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return paymentService.updateStatus(paymentId, request);
    }
}
