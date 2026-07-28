package com.chaukz.store.controller;
import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.request.UserRequest;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.dto.response.UserResponse;
import com.chaukz.store.model.User;
import com.chaukz.store.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
public class UserController {
    private final UserService UserService;
    public UserController (UserService userService){
        this.UserService = userService;

    }
    //public endpoints
    @GetMapping("/api/users")
    public List<UserResponse> getAll(@RequestParam(required = false) Long UserId) {
        if (UserId != null) {
            return UserService.getByUserId(UserId);
        }
        return UserService.getAll();
    }

    @PutMapping ("/api/users/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return UserService.update(id,request);
    }

    //admin endpoints
    // Admin endpoints

    @PostMapping("/api/admin/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse created = UserService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/users/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return UserService.update(id, request);
    }

    @DeleteMapping("/api/admin/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        UserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
