package com.chaukz.store.service;

import com.chaukz.store.dto.request.UpdateOrderStatusRequest;
import com.chaukz.store.dto.response.OrderResponse;
import com.chaukz.store.exception.InvalidOrderStatusException;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.OrderMapper;
import com.chaukz.store.model.Order;
import com.chaukz.store.model.OrderItem;
import com.chaukz.store.model.Payment;
import com.chaukz.store.model.enums.OrderStatus;
import com.chaukz.store.repository.OrderItemRepository;
import com.chaukz.store.repository.OrderRepository;
import com.chaukz.store.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusService {

    /**
     * The fulfillment chain. Each status lists only what may legally follow it.
     * DELIVERED and CANCELLED are terminal - nothing follows them.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING,
                Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED,
                Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PROCESSING,
                Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED,
                Set.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, Set.of());
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, Set.of());
    }

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;

    public OrderStatusService(OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              PaymentRepository paymentRepository,
                              OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        OrderStatus current = order.getOrderStatus();
        OrderStatus target = request.orderStatus();

        if (current == target) {
            throw new InvalidOrderStatusException("Order is already " + current);
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidOrderStatusException(
                    "Cannot move order from " + current + " to " + target
                            + ". Allowed from " + current + ": "
                            + (allowed.isEmpty() ? "none, this status is final" : allowed));
        }

        order.setOrderStatus(target);
        Order saved = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        return orderMapper.toResponse(saved, items, payment);
    }
}
