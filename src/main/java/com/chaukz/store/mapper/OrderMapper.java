package com.chaukz.store.mapper;

import com.chaukz.store.dto.response.OrderItemResponse;
import com.chaukz.store.dto.response.OrderResponse;
import com.chaukz.store.model.Address;
import com.chaukz.store.model.Order;
import com.chaukz.store.model.OrderItem;
import com.chaukz.store.model.Payment;
import com.chaukz.store.model.Product;
import com.chaukz.store.model.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public OrderItemResponse toItemResponse(OrderItem item) {
        ProductVariant variant = item.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;

        // Price comes from the order item row itself, NOT from the variant.
        // This is the price as it was at purchase time.
        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

        return new OrderItemResponse(
                item.getId(),
                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                variant != null ? variant.getSize() : null,
                variant != null ? variant.getColor() : null,
                price,
                quantity,
                price.multiply(BigDecimal.valueOf(quantity))
        );
    }

    public OrderResponse toResponse(Order order, List<OrderItem> items, Payment payment) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        int itemCount = itemResponses.stream()
                .mapToInt(OrderItemResponse::quantity)
                .sum();

        return new OrderResponse(
                order.getId(),
                order.getUser() != null ? order.getUser().getId() : null,
                order.getAddress() != null ? order.getAddress().getId() : null,
                formatAddress(order.getAddress()),
                order.getOrderStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                itemResponses,
                itemCount,
                payment != null ? payment.getPaymentStatus() : null,
                payment != null ? payment.getPaymentMethod() : null
        );
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return null;
        }
        return String.join(", ",
                address.getStreet(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getCountry());
    }
}
