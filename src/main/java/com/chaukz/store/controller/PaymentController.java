package com.chaukz.store.controller;

import com.chaukz.store.dto.request.UpdatePaymentStatusRequest;
import com.chaukz.store.dto.response.PageResponse;
import com.chaukz.store.dto.response.PaymentResponse;
import com.chaukz.store.model.enums.PaymentStatus;
import com.chaukz.store.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<PaymentResponse> getAll(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return paymentService.getAll(status, pageable);
    }

    @PutMapping("/api/admin/payments/{paymentId}/status")
    public PaymentResponse updateStatus(@PathVariable Long paymentId,
                                        @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return paymentService.updateStatus(paymentId, request);
    }
}
