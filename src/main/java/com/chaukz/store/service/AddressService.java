package com.chaukz.store.service;

import com.chaukz.store.dto.request.AddressRequest;
import com.chaukz.store.dto.response.AddressResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.AddressMapper;
import com.chaukz.store.model.Address;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final CurrentUserService currentUserService;

    public AddressService(AddressRepository addressRepository,
                          AddressMapper addressMapper,
                          CurrentUserService currentUserService) {
        this.addressRepository = addressRepository;
        this.addressMapper = addressMapper;
        this.currentUserService = currentUserService;
    }

    public List<AddressResponse> getMyAddresses() {
        Long userId = currentUserService.getCurrentUserId();
        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    public AddressResponse create(AddressRequest request) {
        User user = currentUserService.getCurrentUser();
        Address address = addressMapper.toEntity(user, request);
        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    public AddressResponse update(Long addressId, AddressRequest request) {
        Address address = findAddressOwnedByCurrentUser(addressId);
        addressMapper.updateEntity(address, request);
        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    public void delete(Long addressId) {
        Address address = findAddressOwnedByCurrentUser(addressId);
        addressRepository.delete(address);
    }

    /**
     * Looks up the address AND proves it belongs to whoever is asking,
     * unless they're an admin. Returns 404 rather than 403 either way,
     * so a stranger can't use the response to learn the id exists.
     */
    private Address findAddressOwnedByCurrentUser(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        boolean isOwner = address.getUser() != null
                && address.getUser().getId() != null
                && address.getUser().getId().equals(currentUserService.getCurrentUserId());

        if (!isOwner && !currentUserService.isAdmin()) {
            throw new ResourceNotFoundException("Address not found with id: " + addressId);
        }

        return address;
    }
}
