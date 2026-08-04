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
import com.chaukz.store.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductVariantRepository productVariantRepository,
                        AddressRepository addressRepository,
                        UserRepository userRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * Turns a cart into an order. Every write below happens in one transaction:
     * if any step throws, none of it is persisted.
     */
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + request.addressId()));

        assertAddressBelongsToUser(address, user);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new InvalidOrderStatusException("Cannot checkout with an empty cart");
        }

        // Re-check stock. The cart validated it when items were added, but that
        // may have been days ago and someone else may have bought the last one.
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

            // Snapshot the price. From here on this line never changes,
            // even if the variant is repriced tomorrow.
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

    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    public OrderResponse getById(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        return buildResponse(order);
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Cancelling puts the reserved stock back. Only allowed before the order
     * has shipped — once it is out the door, stock is genuinely gone.
     */
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = findOrderOrThrow(orderId);

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

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void assertAddressBelongsToUser(Address address, User user) {
        boolean belongs = address.getUser() != null
                && address.getUser().getId() != null
                && address.getUser().getId().equals(user.getId());

        if (!belongs) {
            throw new ResourceNotFoundException(
                    "Address not found with id: " + address.getId());
        }
    }
}
