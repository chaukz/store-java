package com.chaukz.store.dto.response;

public record AddressResponse(
        Long id,
        Long userId,
        String street,
        String city,
        String province,
        String postalCode,
        String country
) {
}