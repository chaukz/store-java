package com.chaukz.store.mapper;

import com.chaukz.store.dto.request.AdminUserRequest;
import com.chaukz.store.dto.request.RegisterRequest;
import com.chaukz.store.dto.request.UserRequest;
import com.chaukz.store.dto.response.UserResponse;
import com.chaukz.store.model.User;
import com.chaukz.store.model.enums.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    // ----- registration (public, always ROLE_CUSTOMER) -----

    public User toEntity(RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setDob(request.dob());
        user.setRole(Role.CUSTOMER);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    // ----- self-service update (/api/users/me) -----
    // Never touches email or role. Password is applied by the caller
    // only when present, since UserRequest.password() is optional here.

    public void updateEntity(User user, UserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setDob(request.dob());
    }

    // ----- admin create/update (/api/admin/users/**) -----

    public User toEntity(AdminUserRequest request) {
        User user = new User();
        applyAdminRequest(user, request);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public void updateEntity(User user, AdminUserRequest request) {
        applyAdminRequest(user, request);
    }

    private void applyAdminRequest(User user, AdminUserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setDob(request.dob());
        user.setRole(request.role());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getDob(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
