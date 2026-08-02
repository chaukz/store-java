package com.chaukz.store.mapper;

import com.chaukz.store.dto.response.CartItemResponse;
import com.chaukz.store.dto.response.CartResponse;
import com.chaukz.store.model.Cart;
import com.chaukz.store.model.CartItem;
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    public CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;

        // Price is read live from the variant, never stored on the cart item.
        BigDecimal unitPrice = (variant != null && variant.getPrice() != null)
                ? variant.getPrice()
                : BigDecimal.ZERO;

        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        return new CartItemResponse(
                item.getId(),
                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                variant != null ? variant.getSize() : null,
                variant != null ? variant.getColor() : null,
                unitPrice,
                quantity,
                lineTotal
        );
    }

    public CartResponse toResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = itemResponses.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return new CartResponse(
                cart.getId(),
                cart.getUser() != null ? cart.getUser().getId() : null,
                itemResponses,
                itemCount,
                total
        );
    }
}