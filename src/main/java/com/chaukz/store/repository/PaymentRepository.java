package com.chaukz.store.repository;

import com.chaukz.store.model.Payment;
import com.chaukz.store.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
}
