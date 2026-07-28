package com.chaukz.store.service;

import com.chaukz.store.dto.request.AddressRequest;
import com.chaukz.store.dto.response.AddressResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.AddressMapper;
import com.chaukz.store.model.Address;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.AddressRepository;
import com.chaukz.store.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository,
                          UserRepository userRepository,
                          AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.addressMapper = addressMapper;
    }

    public List<AddressResponse> getByUserId(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    public AddressResponse getById(Long id) {
        Address address = findAddressOrThrow(id);
        return addressMapper.toResponse(address);
    }

    public AddressResponse create(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = addressMapper.toEntity(user, request);
        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    public AddressResponse update(Long addressId, AddressRequest request) {
        Address address = findAddressOrThrow(addressId);
        addressMapper.updateEntity(address, request);
        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    public void delete(Long addressId) {
        Address address = findAddressOrThrow(addressId);
        addressRepository.delete(address);
    }

    private Address findAddressOrThrow(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
    }
}