package com.chaukz.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(

        @NotNull(message = "Category id is required")
        Long categoryId,

        @NotBlank(message = "Product name is required")
        String name,

        String description,

        String sku,

        String brand,

        Boolean active
) {
}