package com.chaukz.store.controller;

import com.chaukz.store.dto.request.AddToCartRequest;
import com.chaukz.store.dto.request.UpdateCartItemRequest;
import com.chaukz.store.dto.response.CartResponse;
import com.chaukz.store.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/users/{userId}/cart")
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/api/users/{userId}/cart/items")
    public CartResponse addItem(@PathVariable Long userId,
                                @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(userId, request);
    }

    @PutMapping("/api/users/{userId}/cart/items/{cartItemId}")
    public CartResponse updateItem(@PathVariable Long userId,
                                   @PathVariable Long cartItemId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(userId, cartItemId, request);
    }

    @DeleteMapping("/api/users/{userId}/cart/items/{cartItemId}")
    public CartResponse removeItem(@PathVariable Long userId,
                                   @PathVariable Long cartItemId) {
        return cartService.removeItem(userId, cartItemId);
    }

    @DeleteMapping("/api/users/{userId}/cart")
    public CartResponse clearCart(@PathVariable Long userId) {
        return cartService.clearCart(userId);
    }
}