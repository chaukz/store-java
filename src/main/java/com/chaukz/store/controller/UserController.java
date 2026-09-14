package com.chaukz.store.controller;

import com.chaukz.store.dto.request.AdminUserRequest;
import com.chaukz.store.dto.request.UserRequest;
import com.chaukz.store.dto.response.UserResponse;
import com.chaukz.store.service.CurrentUserService;
import com.chaukz.store.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    // Self-service: always resolves the id from the authenticated
    // caller's token, never from a path parameter. This is the only
    // profile endpoint a regular user can reach.

    @GetMapping("/api/users/me")
    public UserResponse getOwnProfile() {
        return userService.getById(currentUserService.getCurrentUserId());
    }

    @PutMapping("/api/users/me")
    public UserResponse updateOwnProfile(@Valid @RequestBody UserRequest request) {
        return userService.updateOwnProfile(currentUserService.getCurrentUserId(), request);
    }

    // Admin-only: listing/reading/editing arbitrary users, or changing
    // a role, requires ROLE_ADMIN (enforced in SecurityConfig).

    @GetMapping("/api/admin/users")
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @GetMapping("/api/admin/users/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping("/api/admin/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody AdminUserRequest request) {
        UserResponse created = userService.adminCreate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/users/{id}")
    public UserResponse adminUpdate(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request) {
        return userService.adminUpdate(id, request);
    }

    @DeleteMapping("/api/admin/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
