package com.chaukz.store.service;

import com.chaukz.store.dto.request.AddToCartRequest;
import com.chaukz.store.dto.response.CartResponse;
import com.chaukz.store.exception.InsufficientStockException;
import com.chaukz.store.mapper.CartMapper;
import com.chaukz.store.model.Cart;
import com.chaukz.store.model.CartItem;
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.CartItemRepository;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.repository.ProductVariantRepository;
import com.chaukz.store.repository.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CartService. Every dependency is mocked - no database,
 * no Spring context. This tests the DECISIONS (merge vs create, stock
 * math), not whether JPA or Postgres work.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private UserRepository userRepository;

    private CartService cartService;

    private Cart cart;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        // CartMapper has no dependencies of its own - it's pure translation
        // logic, so a real instance is more meaningful here than a mock.
        CartMapper cartMapper = new CartMapper();

        cartService = new CartService(
                cartRepository, cartItemRepository, productVariantRepository,
                userRepository, cartMapper);

        User user = new User();
        user.setId(1L);

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

        // Every test starts from an existing cart, so findOrCreateCart never
        // needs to fall into its "create one" branch.
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    }

    @Test
    void addItem_toEmptyCart_createsNewCartItem() {
        when(productVariantRepository.findById(20L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 20L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(10L)).thenAnswer(inv -> {
            CartItem item = new CartItem();
            item.setId(100L);
            item.setCart(cart);
            item.setProductVariant(variant);
            item.setQuantity(2);
            return List.of(item);
        });

        CartResponse response = cartService.addItem(1L, new AddToCartRequest(20L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.itemCount()).isEqualTo(2);
    }

    @Test
    void addItem_sameVariantTwice_mergesQuantityInsteadOfDuplicating() {
        CartItem existing = new CartItem();
        existing.setId(100L);
        existing.setCart(cart);
        existing.setProductVariant(variant);
        existing.setQuantity(2);

        when(productVariantRepository.findById(20L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 20L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(existing));

        // Adding 3 more to an existing quantity of 2 must result in ONE
        // item with quantity 5 - not a second row.
        cartService.addItem(1L, new AddToCartRequest(20L, 3));

        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(cartItemRepository, org.mockito.Mockito.never()).save(
                org.mockito.ArgumentMatchers.argThat(item -> item != existing));
    }

    @Test
    void addItem_requestAloneExceedsStock_throwsInsufficientStock() {
        // stock is 10, requesting 999 in one go
        when(productVariantRepository.findById(20L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, new AddToCartRequest(20L, 999)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void addItem_existingPlusRequestedExceedsStock_throwsEvenThoughRequestedAloneWouldFit() {
        // 8 already in the cart, stock is 10. Requesting 5 more is fine on
        // its own, but 8 + 5 = 13 exceeds stock - this is the case that
        // proves the check validates the TOTAL, not just the new request.
        CartItem existing = new CartItem();
        existing.setId(100L);
        existing.setCart(cart);
        existing.setProductVariant(variant);
        existing.setQuantity(8);

        when(productVariantRepository.findById(20L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(10L, 20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItem(1L, new AddToCartRequest(20L, 5)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void removeItem_deletesTheItem() {
        CartItem item = new CartItem();
        item.setId(100L);
        item.setCart(cart);

        when(cartItemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        cartService.removeItem(1L, 100L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void clearCart_deletesAllItemsButKeepsTheCartRow() {
        CartItem item1 = new CartItem();
        item1.setId(100L);
        CartItem item2 = new CartItem();
        item2.setId(101L);
        List<CartItem> items = List.of(item1, item2);

        // First call (fetch items to delete) returns both; second call
        // (building the response afterward) returns empty - simulating
        // the post-deletion state.
        when(cartItemRepository.findByCartId(10L)).thenReturn(items, List.of());

        CartResponse response = cartService.clearCart(1L);

        verify(cartItemRepository).deleteAll(items);
        verify(cartRepository, org.mockito.Mockito.never()).delete(any());
        assertThat(response.items()).isEmpty();
    }
}