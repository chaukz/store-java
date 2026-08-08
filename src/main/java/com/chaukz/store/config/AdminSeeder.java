package com.chaukz.store.config;

import com.chaukz.store.model.Cart;
import com.chaukz.store.model.User;
import com.chaukz.store.model.enums.Role;
import com.chaukz.store.repository.CartRepository;
import com.chaukz.store.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Runs once at startup. Without this, nobody could ever create the first
 * admin account - creating a user requires ADMIN, and there is no admin
 * until a user is created. This breaks that deadlock.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@store.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository,
                       CartRepository cartRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        User admin = new User();
        admin.setFirstName("Store");
        admin.setLastName("Admin");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setPhone("0000000000");
        admin.setDob(LocalDate.of(2000, 1, 1));
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(admin);

        Cart cart = new Cart();
        cart.setUser(saved);
        cart.setCreatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        System.out.println("Seeded admin account: " + ADMIN_EMAIL + " / " + ADMIN_PASSWORD);
    }
}
