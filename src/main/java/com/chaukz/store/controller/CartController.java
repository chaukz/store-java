package com.chaukz.store.controller;

import com.chaukz.store.dto.request.AddToCartRequest;
import com.chaukz.store.dto.request.UpdateCartItemRequest;
import com.chaukz.store.dto.response.CartResponse;
import com.chaukz.store.service.CartService;
import com.chaukz.store.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/cart")
    public CartResponse getCart() {
        return cartService.getCart(currentUserService.getCurrentUserId());
    }

    @PostMapping("/api/cart/items")
    public CartResponse addItem(@Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(currentUserService.getCurrentUserId(), request);
    }

    @PutMapping("/api/cart/items/{cartItemId}")
    public CartResponse updateItem(@PathVariable Long cartItemId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(currentUserService.getCurrentUserId(), cartItemId, request);
    }

    @DeleteMapping("/api/cart/items/{cartItemId}")
    public CartResponse removeItem(@PathVariable Long cartItemId) {
        return cartService.removeItem(currentUserService.getCurrentUserId(), cartItemId);
    }

    @DeleteMapping("/api/cart")
    public CartResponse clearCart() {
        return cartService.clearCart(currentUserService.getCurrentUserId());
    }
}
