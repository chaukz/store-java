package com.chaukz.store.service;

import com.chaukz.store.dto.request.UpdatePaymentStatusRequest;
import com.chaukz.store.dto.response.PaymentResponse;
import com.chaukz.store.exception.InvalidOrderStatusException;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.PaymentMapper;
import com.chaukz.store.model.Order;
import com.chaukz.store.model.Payment;
import com.chaukz.store.model.enums.OrderStatus;
import com.chaukz.store.model.enums.PaymentStatus;
import com.chaukz.store.repository.OrderRepository;
import com.chaukz.store.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentService. The transition map IS the business logic
 * here - these tests prove which moves are legal, which are rejected, and
 * that a status change correctly ripples into the order it belongs to.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;

    private PaymentService paymentService;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        PaymentMapper paymentMapper = new PaymentMapper();
        paymentService = new PaymentService(paymentRepository, orderRepository, paymentMapper);

        order = new Order();
        order.setId(500L);
        order.setOrderStatus(OrderStatus.PENDING);

        payment = new Payment();
        payment.setId(700L);
        payment.setOrder(order);
        payment.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Test
    void updateStatus_pendingToPaid_setsPaidAtAndConfirmsThePendingOrder() {
        when(paymentRepository.findById(700L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePaymentStatusRequest request =
                new UpdatePaymentStatusRequest(PaymentStatus.PAID, "TXN-123");

        PaymentResponse response = paymentService.updateStatus(700L, request);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateStatus_paidToRefunded_cancelsTheOrder() {
        payment.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.CONFIRMED);

        when(paymentRepository.findById(700L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.REFUNDED, null);

        paymentService.updateStatus(700L, request);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateStatus_pendingDirectlyToRefunded_rejectedByTheTransitionMap() {
        when(paymentRepository.findById(700L)).thenReturn(Optional.of(payment));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.REFUNDED, null);

        // PENDING can only go to PAID or FAILED - REFUNDED requires PAID first.
        assertThatThrownBy(() -> paymentService.updateStatus(700L, request))
                .isInstanceOf(InvalidOrderStatusException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updateStatus_sameStatusAsCurrent_rejected() {
        when(paymentRepository.findById(700L)).thenReturn(Optional.of(payment));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.PENDING, null);

        assertThatThrownBy(() -> paymentService.updateStatus(700L, request))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void updateStatus_refundedIsTerminal_nothingCanFollowIt() {
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findById(700L)).thenReturn(Optional.of(payment));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(PaymentStatus.PENDING, null);

        assertThatThrownBy(() -> paymentService.updateStatus(700L, request))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void getByOrderId_noPaymentExists_throwsResourceNotFound() {
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByOrderId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
