package com.chaukz.store.service;

import com.chaukz.store.dto.request.AddToCartRequest;
import com.chaukz.store.dto.request.UpdateCartItemRequest;
import com.chaukz.store.dto.response.CartResponse;
import com.chaukz.store.exception.InsufficientStockException;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.CartMapper;
import com.chaukz.store.model.Cart;
import com.chaukz.store.model.CartItem;
import com.chaukz.store.model.ProductVariant;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.CartItemRepository;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.repository.ProductVariantRepository;
import com.chaukz.store.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository productVariantRepository,
                       UserRepository userRepository,
                       CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
    }

    public CartResponse getCart(Long userId) {
        Cart cart = findOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return cartMapper.toResponse(cart, items);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Cart cart = findOrCreateCart(userId);

        ProductVariant variant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product variant not found with id: " + request.productVariantId()));

        // Is this variant already sitting in the cart?
        CartItem item = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        // Additive: existing quantity + requested quantity.
        int existingQuantity = (item != null && item.getQuantity() != null) ? item.getQuantity() : 0;
        int newQuantity = existingQuantity + request.quantity();

        // Validate the TOTAL, not just the requested amount.
        validateStock(variant, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProductVariant(variant);
        }
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = findOrCreateCart(userId);
        CartItem item = findItemOrThrow(cartItemId);
        assertItemBelongsToCart(item, cart);

        // Absolute set, unlike addItem which is additive.
        validateStock(item.getProductVariant(), request.quantity());

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = findOrCreateCart(userId);
        CartItem item = findItemOrThrow(cartItemId);
        assertItemBelongsToCart(item, cart);

        cartItemRepository.delete(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = findOrCreateCart(userId);

        // Empties the cart but keeps the cart row itself.
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cartItemRepository.deleteAll(items);

        return getCart(userId);
    }

    // ----- helpers -----

    /**
     * Carts are normally created at user registration. This falls back to creating
     * one on demand so that users who predate that rule still work.
     */
    private Cart findOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "User not found with id: " + userId));

                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setCreatedAt(LocalDateTime.now());
                    return cartRepository.save(cart);
                });
    }

    private CartItem findItemOrThrow(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));
    }

    /**
     * Stops one user from touching another user's cart items by guessing an id.
     * Reports 404 rather than 403 so we don't leak whether the id exists at all.
     */
    private void assertItemBelongsToCart(CartItem item, Cart cart) {
        boolean belongs = item.getCart() != null
                && item.getCart().getId() != null
                && item.getCart().getId().equals(cart.getId());

        if (!belongs) {
            throw new ResourceNotFoundException(
                    "Cart item not found with id: " + item.getId());
        }
    }

    private void validateStock(ProductVariant variant, int requestedQuantity) {
        int available = (variant != null && variant.getStockQuantity() != null)
                ? variant.getStockQuantity()
                : 0;

        if (requestedQuantity > available) {
            throw new InsufficientStockException(
                    "Only " + available + " left in stock, but " + requestedQuantity + " requested");
        }
    }
}