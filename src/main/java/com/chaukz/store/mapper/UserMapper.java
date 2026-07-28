package com.chaukz.store.mapper;

import com.chaukz.store.dto.request.UserRequest;
import com.chaukz.store.dto.response.UserResponse;
import com.chaukz.store.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        User user = new User();
        applyRequest(user, request);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public void updateEntity(User user, UserRequest request) {
        applyRequest(user, request);
    }

    private void applyRequest(User user, UserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(request.password());
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