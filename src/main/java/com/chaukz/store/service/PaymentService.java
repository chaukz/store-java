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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {

    /**
     * Which payment statuses can follow which. A terminal status has no exits.
     */
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PaymentStatus.PENDING, Set.of(PaymentStatus.PAID, PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.PAID, Set.of(PaymentStatus.REFUNDED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.FAILED, Set.of(PaymentStatus.PENDING));
        ALLOWED_TRANSITIONS.put(PaymentStatus.REFUNDED, Set.of());
    }

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentMapper = paymentMapper;
    }

    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment found for order id: " + orderId));
        return paymentMapper.toResponse(payment);
    }

    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    /**
     * Updating a payment status also moves the order. A paid order becomes
     * CONFIRMED; a refunded one becomes CANCELLED.
     */
    @Transactional
    public PaymentResponse updateStatus(Long paymentId, UpdatePaymentStatusRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));

        PaymentStatus current = payment.getPaymentStatus();
        PaymentStatus target = request.paymentStatus();

        if (current == target) {
            throw new InvalidOrderStatusException("Payment is already " + current);
        }

        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidOrderStatusException(
                    "Cannot move payment from " + current + " to " + target);
        }

        payment.setPaymentStatus(target);

        if (request.transactionId() != null) {
            payment.setTransactionId(request.transactionId());
        }

        if (target == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment saved = paymentRepository.save(payment);

        applyOrderSideEffect(saved.getOrder(), target);

        return paymentMapper.toResponse(saved);
    }

    private void applyOrderSideEffect(Order order, PaymentStatus target) {
        if (order == null) {
            return;
        }

        if (target == PaymentStatus.PAID && order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        if (target == PaymentStatus.REFUNDED && order.getOrderStatus() != OrderStatus.CANCELLED) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }
}
