package com.chaukz.store.mapper;

import com.chaukz.store.dto.response.PaymentResponse;
import com.chaukz.store.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder() != null ? payment.getOrder().getId() : null,
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getPaidAt()
        );
    }
}
