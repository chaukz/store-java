package com.chaukz.store.service;

import com.chaukz.store.dto.request.CheckoutRequest;
import com.chaukz.store.dto.response.OrderResponse;
import com.chaukz.store.dto.response.PageResponse;

import com.chaukz.store.exception.InsufficientStockException;
import com.chaukz.store.exception.InvalidOrderStatusException;
import com.chaukz.store.exception.ResourceNotFoundException;

import com.chaukz.store.mapper.OrderMapper;
import com.chaukz.store.model.Address;
import com.chaukz.store.model.Cart;
import com.chaukz.store.model.CartItem;
import com.chaukz.store.model.Order;
import com.chaukz.store.model.OrderItem;
import com.chaukz.store.model.Payment;
import com.chaukz.store.model.ProductVariant;
import com.chaukz.store.model.User;

import com.chaukz.store.model.enums.OrderStatus;
import com.chaukz.store.model.enums.PaymentStatus;
import com.chaukz.store.repository.AddressRepository;
import com.chaukz.store.repository.CartItemRepository;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.repository.OrderItemRepository;
import com.chaukz.store.repository.OrderRepository;
import com.chaukz.store.repository.PaymentRepository;
import com.chaukz.store.repository.ProductVariantRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AddressRepository addressRepository;
    private final OrderMapper orderMapper;
    private final CurrentUserService currentUserService;


    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductVariantRepository productVariantRepository,
                        AddressRepository addressRepository,
                        OrderMapper orderMapper,
                        CurrentUserService currentUserService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.addressRepository = addressRepository;
        this.orderMapper = orderMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        User user = currentUserService.getCurrentUser();

        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + request.addressId()));

        assertAddressBelongsToUser(address, user);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for current user"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new InvalidOrderStatusException("Cannot checkout with an empty cart");
        }

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            int available = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
            int wanted = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;

            if (wanted > available) {
                throw new InsufficientStockException(
                        "Only " + available + " left of variant " + variant.getId()
                                + ", but " + wanted + " requested");
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotal(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            int quantity = cartItem.getQuantity();
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(unitPrice);
            orderItems.add(orderItemRepository.save(orderItem));

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));

            variant.setStockQuantity(variant.getStockQuantity() - quantity);
            productVariantRepository.save(variant);
        }

        savedOrder.setTotal(total);
        savedOrder = orderRepository.save(savedOrder);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setAmount(total);
        Payment savedPayment = paymentRepository.save(payment);

        cartItemRepository.deleteAll(cartItems);

        return orderMapper.toResponse(savedOrder, orderItems, savedPayment);
    }

    public List<OrderResponse> getMyOrders() {
        Long userId = currentUserService.getCurrentUserId();
        return buildResponses(orderRepository.findByUserId(userId));
    }

    public OrderResponse getById(Long orderId) {
        Order order = findOrderOwnedByCurrentUserOrAdmin(orderId);
        return buildResponses(List.of(order)).get(0);
    }

    /**
     * Admin order list, paginated. Pass a status to see only orders in that
     * state, or leave it null to see everything.
     */
    public PageResponse<OrderResponse> getAll(OrderStatus status, Pageable pageable) {
        Page<Order> orders = (status != null)
                ? orderRepository.findByOrderStatus(status, pageable)
                : orderRepository.findAll(pageable);

        List<OrderResponse> content = buildResponses(orders.getContent());
        Page<OrderResponse> responsePage = new PageImpl<>(content, pageable, orders.getTotalElements());
        return PageResponse.from(responsePage);
    }

    /**
     * Builds responses for a whole list of orders in a fixed number of
     * queries, no matter how many orders there are. This is the actual
     * N+1 fix: instead of asking "give me this order's items" and "give
     * me this order's payment" once per order, we ask each question
     * exactly once, with a list of every order id we need, then match
     * the results back up in memory using the two Maps below.
     */
    private List<OrderResponse> buildResponses(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository
                .findByOrderIdInWithVariantAndProduct(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        Map<Long, Payment> paymentByOrderId = paymentRepository
                .findByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getId(), payment -> payment));

        return orders.stream()
                .map(order -> orderMapper.toResponse(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of()),
                        paymentByOrderId.get(order.getId())))
                .toList();
    }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = findOrderOwnedByCurrentUserOrAdmin(orderId);

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Order " + orderId + " is already cancelled");
        }

        boolean cancellable = order.getOrderStatus() == OrderStatus.PENDING
                || order.getOrderStatus() == OrderStatus.CONFIRMED
                || order.getOrderStatus() == OrderStatus.PROCESSING;

        if (!cancellable) {
            throw new InvalidOrderStatusException(
                    "Cannot cancel an order with status " + order.getOrderStatus());
        }

        List<OrderItem> items = orderItemRepository.findByOrderIdInWithVariantAndProduct(List.of(orderId));
        for (OrderItem item : items) {
            ProductVariant variant = item.getProductVariant();
            if (variant != null) {
                int current = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
                variant.setStockQuantity(current + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });

        return buildResponse(saved);
    }

    // ----- helpers -----

    private OrderResponse buildResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return orderMapper.toResponse(order, items, payment);
    }

    private Order findOrderOwnedByCurrentUserOrAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isOwner = order.getUser() != null
                && order.getUser().getId() != null
                && order.getUser().getId().equals(currentUserService.getCurrentUserId());

        if (!isOwner && !currentUserService.isAdmin()) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return order;
    }

    private void assertAddressBelongsToUser(Address address, User user) {
        boolean belongs = address.getUser() != null
                && address.getUser().getId() != null
                && address.getUser().getId().equals(user.getId());

        if (!belongs) {
            throw new ResourceNotFoundException("Address not found with id: " + address.getId());
        }
    }
}
