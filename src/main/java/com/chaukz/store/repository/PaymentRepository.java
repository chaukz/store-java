package com.chaukz.store.repository;

import com.chaukz.store.model.Payment;
import com.chaukz.store.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    // Batched version of findByOrderId - one query for every order on a
    // page, instead of one query per order.
    List<Payment> findByOrderIdIn(List<Long> orderIds);

    Page<Payment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
}