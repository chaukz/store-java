package com.chaukz.store.mapper;

import com.chaukz.store.dto.request.AddressRequest;
import com.chaukz.store.dto.response.AddressResponse;
import com.chaukz.store.model.Address;
import com.chaukz.store.model.User;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(User user, AddressRequest request) {
        Address address = new Address();
        address.setUser(user);
        applyRequest(address, request);
        return address;
    }

    public void updateEntity(Address address, AddressRequest request) {
        applyRequest(address, request);
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setProvince(request.province());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
    }

    public AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getUser() != null ? address.getUser().getId() : null,
                address.getStreet(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}