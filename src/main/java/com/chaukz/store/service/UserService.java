package com.chaukz.store.service;

import com.chaukz.store.dto.request.UserRequest;
import com.chaukz.store.dto.response.UserResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.UserMapper;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.model.Cart;
import java.util.List;
import java.time.LocalDateTime;


@Service
public class UserService {

    private final UserRepository UserRepository;
    private final UserMapper UserMapper;
    private final CartRepository CartRepository;
    public UserService(UserRepository UserRepository, UserMapper UserMapper, CartRepository CartRepository) {
        this.UserRepository = UserRepository;
        this.UserMapper = UserMapper;
        this.CartRepository = CartRepository;
    }

    public List<UserResponse> getAll() {
        return UserRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    public List<UserResponse> getByUserId(Long UserId) {
        return UserRepository.findById(UserId)
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    public List<UserResponse> search(String query) {
        return UserRepository.findByEmail(query)
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    public UserResponse getById(Long id) {
        User user = findUserOrThrow(id);
        return UserMapper.toResponse(user);
    }

    public UserResponse create(UserRequest request) {
        User user = UserMapper.toEntity(request);
        User saved = UserRepository.save(user);
        Cart cart = new Cart();
        cart.setUser(saved);
        cart.setCreatedAt(LocalDateTime.now());
        CartRepository.save(cart);

        return UserMapper.toResponse(saved);
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = findUserOrThrow(id);
        UserMapper.updateEntity(user, request);
        User saved = UserRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    public void delete(Long id) {
        User user = findUserOrThrow(id);
        UserRepository.delete(user);
    }

    private User findUserOrThrow(Long id) {
        return UserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}