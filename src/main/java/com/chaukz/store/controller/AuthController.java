package com.chaukz.store.controller;

import com.chaukz.store.dto.request.LoginRequest;
import com.chaukz.store.dto.request.RegisterRequest;
import com.chaukz.store.dto.response.AuthResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.model.User;
import com.chaukz.store.repository.UserRepository;
import com.chaukz.store.service.JwtService;
import com.chaukz.store.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole());
    }

    /**
     * Public self-registration. Always creates a ROLE_CUSTOMER account -
     * see UserService.register()/UserMapper for why there's no way to
     * request a different role here. Logs the new user straight in so
     * the frontend doesn't need a separate post-signup login step.
     */
    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User saved = userService.register(request);
        String token = jwtService.generateToken(saved.getEmail(), saved.getRole().name());
        AuthResponse response = new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
