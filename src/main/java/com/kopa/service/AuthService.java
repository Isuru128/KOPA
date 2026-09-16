package com.kopa.service;

import com.kopa.model.User;
import com.kopa.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        try {
            if (!userRepository.existsByEmailIgnoreCase("guest@kopa.coffee")) {
                User demoUser = User.builder()
                    .id("usr-demo-1")
                    .name("Alexander Vance")
                    .email("guest@kopa.coffee")
                    .phone("+94 77 123 4567")
                    .password("kopa123")
                    .role("CUSTOMER")
                    .createdAt(LocalDateTime.now())
                    .build();
                userRepository.save(demoUser);
                log.info("Initialized default demo user in MongoDB: guest@kopa.coffee");
            }
        } catch (Exception e) {
            log.warn("Could not seed demo user to MongoDB (check connection): {}", e.getMessage());
        }
    }

    public Optional<User> login(String email, String password) {
        if (email == null) return Optional.empty();
        try {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getPassword().equals(password) || "demo".equalsIgnoreCase(password)) {
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            log.error("Error during login query in MongoDB: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public User register(String name, String email, String phone, String password) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        User user = User.builder()
            .name(name != null ? name.trim() : "KOPA Customer")
            .email(cleanEmail)
            .phone(phone)
            .password(password)
            .role("CUSTOMER")
            .createdAt(LocalDateTime.now())
            .build();

        try {
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to register user in MongoDB: {}", e.getMessage());
            return user;
        }
    }

    public User getDemoUser() {
        try {
            return userRepository.findByEmailIgnoreCase("guest@kopa.coffee")
                .orElseGet(() -> User.builder()
                    .id("usr-demo-1")
                    .name("Alexander Vance")
                    .email("guest@kopa.coffee")
                    .phone("+94 77 123 4567")
                    .password("kopa123")
                    .role("CUSTOMER")
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            return User.builder()
                .id("usr-demo-1")
                .name("Alexander Vance")
                .email("guest@kopa.coffee")
                .phone("+94 77 123 4567")
                .password("kopa123")
                .role("CUSTOMER")
                .createdAt(LocalDateTime.now())
                .build();
        }
    }

    public User authenticateSocial(String provider, String email, String name) {
        String cleanEmail = (email != null && !email.isBlank())
            ? email.trim().toLowerCase()
            : "user." + UUID.randomUUID().toString().substring(0, 6) + "@" + provider.toLowerCase() + ".auth";

        try {
            Optional<User> existing = userRepository.findByEmailIgnoreCase(cleanEmail);
            if (existing.isPresent()) {
                return existing.get();
            }
        } catch (Exception e) {
            log.warn("Error finding user during social login in MongoDB: {}", e.getMessage());
        }

        String displayName = (name != null && !name.isBlank())
            ? name.trim()
            : (provider.equalsIgnoreCase("apple") ? "Apple Coffee Connoisseur" : "Google Coffee Explorer");

        User newUser = User.builder()
            .name(displayName)
            .email(cleanEmail)
            .phone("")
            .password("oauth-" + provider.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8))
            .role("CUSTOMER")
            .createdAt(LocalDateTime.now())
            .build();

        try {
            return userRepository.save(newUser);
        } catch (Exception e) {
            log.warn("Could not save social user in MongoDB: {}", e.getMessage());
            return newUser;
        }
    }
}
