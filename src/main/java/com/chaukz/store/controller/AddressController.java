package com.chaukz.store.controller;

import com.chaukz.store.dto.request.AddressRequest;
import com.chaukz.store.dto.response.AddressResponse;
import com.chaukz.store.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/api/users/{userId}/addresses")
    public List<AddressResponse> getByUserId(@PathVariable Long userId) {
        return addressService.getByUserId(userId);
    }

    @PostMapping("/api/users/{userId}/addresses")
    public ResponseEntity<AddressResponse> create(
            @PathVariable Long userId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/addresses/{addressId}")
    public AddressResponse update(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return addressService.update(addressId, request);
    }

    @DeleteMapping("/api/addresses/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable Long addressId) {
        addressService.delete(addressId);
        return ResponseEntity.noContent().build();
    }
}