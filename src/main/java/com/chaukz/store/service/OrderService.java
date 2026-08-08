package com.chaukz.store.service;

import com.chaukz.store.dto.request.CheckoutRequest;
import com.chaukz.store.dto.response.OrderResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Any logged-in user can request any order id - the ownership check is
     * what stops that from leaking someone else's order. Admins bypass it.
     */
    public OrderResponse getById(Long orderId) {
        Order order = findOrderOwnedByCurrentUserOrAdmin(orderId);
        return buildResponse(order);
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::buildResponse)
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

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
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
