package com.chaukz.store.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(

        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Province is required")
        String province,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "Country is required")
        String country
) {
}