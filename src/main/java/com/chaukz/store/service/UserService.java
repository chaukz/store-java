package com.chaukz.store.service;

import com.chaukz.store.dto.request.AdminUserRequest;
import com.chaukz.store.dto.request.RegisterRequest;
import com.chaukz.store.dto.request.UserRequest;
import com.chaukz.store.dto.response.UserResponse;
import com.chaukz.store.exception.DuplicateResourceException;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.UserMapper;
import com.chaukz.store.model.Cart;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       CartRepository cartRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
    }

    // ----- self-service -----

    /**
     * Public registration. Always produces a ROLE_CUSTOMER account - role is
     * set inside UserMapper.toEntity(RegisterRequest), never taken from
     * the request, so there's no field for a caller to smuggle a
     * privilege escalation through.
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(saved);
        cart.setCreatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return saved;
    }

    public UserResponse getById(Long id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    /**
     * Self-service profile update. The id comes from the authenticated
     * caller (CurrentUserService), never from a path parameter, so this
     * can only ever touch the caller's own row. Role and email are not
     * on UserRequest at all, so there's nothing here to escalate.
     */
    public UserResponse updateOwnProfile(Long currentUserId, UserRequest request) {
        User user = findUserOrThrow(currentUserId);
        userMapper.updateEntity(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    // ----- admin -----

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse adminCreate(AdminUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(saved);
        cart.setCreatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return userMapper.toResponse(saved);
    }

    public UserResponse adminUpdate(Long id, AdminUserRequest request) {
        User user = findUserOrThrow(id);
        userMapper.updateEntity(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    public void delete(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    // ----- helpers -----

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
