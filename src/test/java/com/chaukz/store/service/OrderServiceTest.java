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
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import com.chaukz.store.model.User;
import com.chaukz.store.model.enums.OrderStatus;
import com.chaukz.store.model.enums.PaymentMethod;
import com.chaukz.store.model.enums.PaymentStatus;
import com.chaukz.store.repository.AddressRepository;
import com.chaukz.store.repository.CartItemRepository;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.repository.OrderItemRepository;
import com.chaukz.store.repository.OrderRepository;
import com.chaukz.store.repository.PaymentRepository;
import com.chaukz.store.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderService. Checkout touches six repositories in one
 * transaction - these tests prove the DECISIONS around that: validate
 * everything before writing anything, snapshot the price, reduce stock,
 * clear the cart, and enforce ownership on read/cancel afterward.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CurrentUserService currentUserService;

    private OrderService orderService;

    private User user;
    private Address address;
    private Cart cart;
    private ProductVariant variant;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        // OrderMapper is pure translation, no dependencies - real instance
        // is more meaningful than a mock here, same reasoning as CartMapper.
        OrderMapper orderMapper = new OrderMapper();

        orderService = new OrderService(
                orderRepository, orderItemRepository, paymentRepository,
                cartRepository, cartItemRepository, productVariantRepository,
                addressRepository, orderMapper, currentUserService);

        user = new User();
        user.setId(1L);

        address = new Address();
        address.setId(2L);
        address.setUser(user);
        address.setStreet("12 Baobab Ave");
        address.setCity("Mbombela");
        address.setProvince("Mpumalanga");
        address.setPostalCode("1200");
        address.setCountry("South Africa");

        cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        Product product = new Product();
        product.setId(5L);
        product.setName("Running Sneaker");

        variant = new ProductVariant();
        variant.setId(20L);
        variant.setProduct(product);
        variant.setSize("US 9");
        variant.setColor("Black");
        variant.setPrice(new BigDecimal("899.99"));
        variant.setStockQuantity(10);

        cartItem = new CartItem();
        cartItem.setId(100L);
        cartItem.setCart(cart);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(3);
    }

    @Test
    void checkout_happyPath_createsOrderReducesStockAndClearsCart() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(cartItem));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(500L);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem oi = inv.getArgument(0);
            if (oi.getId() == null) oi.setId(900L);
            return oi;
        });
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(700L);
            return p;
        });

        OrderResponse response = orderService.checkout(new CheckoutRequest(2L, PaymentMethod.CARD));

        // Price snapshotted correctly: 3 x 899.99
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("2699.97"));
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.items()).hasSize(1);

        // Stock reduced: started at 10, bought 3
        assertThat(variant.getStockQuantity()).isEqualTo(7);

        // Cart actually cleared
        verify(cartItemRepository).deleteAll(List.of(cartItem));
    }

    @Test
    void checkout_emptyCart_throwsBeforeWritingAnything() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(new CheckoutRequest(2L, PaymentMethod.CARD)))
                .isInstanceOf(InvalidOrderStatusException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_insufficientStock_throwsBeforeWritingAnything() {
        cartItem.setQuantity(20); // stock is only 10

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.checkout(new CheckoutRequest(2L, PaymentMethod.CARD)))
                .isInstanceOf(InsufficientStockException.class);

        // The whole point of validating stock BEFORE creating the order:
        // nothing should have been written yet.
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    void checkout_addressBelongsToSomeoneElse_throwsResourceNotFound() {
        User strangerUser = new User();
        strangerUser.setId(999L);
        address.setUser(strangerUser); // address does NOT belong to the current user

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(addressRepository.findById(2L)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> orderService.checkout(new CheckoutRequest(2L, PaymentMethod.CARD)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getById_ownerCanViewTheirOwnOrder() {
        Order order = buildOrder(500L, user, OrderStatus.PENDING);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(orderItemRepository.findByOrderId(500L)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(500L)).thenReturn(Optional.empty());

        OrderResponse response = orderService.getById(500L);

        assertThat(response.id()).isEqualTo(500L);
    }

    @Test
    void getById_strangerCannotViewSomeoneElsesOrder() {
        Order order = buildOrder(500L, user, OrderStatus.PENDING); // belongs to user id 1

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(currentUserService.getCurrentUserId()).thenReturn(999L); // different user
        when(currentUserService.isAdmin()).thenReturn(false);

        // Must be 404, not 403 - a stranger shouldn't learn the order exists at all.
        assertThatThrownBy(() -> orderService.getById(500L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_restoresStockAndFailsThePendingPayment() {
        Order order = buildOrder(500L, user, OrderStatus.PENDING);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(900L);
        orderItem.setOrder(order);
        orderItem.setProductVariant(variant); // stock currently 10
        orderItem.setQuantity(3);

        Payment payment = new Payment();
        payment.setId(700L);
        payment.setOrder(order);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(orderItemRepository.findByOrderId(500L)).thenReturn(List.of(orderItem));
        when(paymentRepository.findByOrderId(500L)).thenReturn(Optional.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        orderService.cancel(500L);

        assertThat(variant.getStockQuantity()).isEqualTo(13); // 10 + 3 restored
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cancel_alreadyCancelled_throwsWithoutTouchingStock() {
        Order order = buildOrder(500L, user, OrderStatus.CANCELLED);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(currentUserService.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> orderService.cancel(500L))
                .isInstanceOf(InvalidOrderStatusException.class);

        verify(productVariantRepository, never()).save(any());
    }

    private Order buildOrder(Long id, User owner, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setAddress(address);
        order.setOrderStatus(status);
        order.setTotal(new BigDecimal("2699.97"));
        return order;
    }
}
