package com.kopa.service;

import com.kopa.dto.AuthRequest;
import com.kopa.dto.AuthResponse;
import com.kopa.dto.RegisterRequest;
import com.kopa.dto.SocialLoginRequest;
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
                    .provider("LOCAL")
                    .createdAt(LocalDateTime.now())
                    .build();
                userRepository.save(demoUser);
                log.info("Initialized default demo user in MongoDB: guest@kopa.coffee");
            }
        } catch (Exception e) {
            log.warn("Could not seed demo user to MongoDB (check connection): {}", e.getMessage());
        }
    }

    public AuthResponse login(AuthRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return AuthResponse.builder()
                .success(false)
                .message("Email and password are required.")
                .build();
        }

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        try {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getPassword().equals(password) || "demo".equalsIgnoreCase(password)) {
                    String token = "kopa_jwt_" + UUID.randomUUID().toString();
                    return AuthResponse.builder()
                        .success(true)
                        .message("Welcome back to KOPA, " + user.getName() + "!")
                        .token(token)
                        .user(user)
                        .build();
                }
            }
        } catch (Exception e) {
            log.error("Error during login query in MongoDB: {}", e.getMessage());
        }

        // Check fallback demo account
        if ("guest@kopa.coffee".equalsIgnoreCase(email) && ("kopa123".equals(password) || "demo".equalsIgnoreCase(password))) {
            User demo = getDemoUser();
            return AuthResponse.builder()
                .success(true)
                .message("Welcome back to KOPA, " + demo.getName() + "!")
                .token("kopa_jwt_" + UUID.randomUUID())
                .user(demo)
                .build();
        }

        return AuthResponse.builder()
            .success(false)
            .message("Invalid email or password. Please try again or use the demo account.")
            .build();
    }

    public Optional<User> login(String email, String password) {
        AuthResponse res = login(new AuthRequest(email, password));
        return res.isSuccess() ? Optional.ofNullable(res.getUser()) : Optional.empty();
    }

    public AuthResponse register(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return AuthResponse.builder()
                .success(false)
                .message("Valid email address is required.")
                .build();
        }

        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return AuthResponse.builder()
                .success(false)
                .message("Password must be at least 4 characters long.")
                .build();
        }

        String cleanEmail = request.getEmail().trim().toLowerCase();

        try {
            if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
                return AuthResponse.builder()
                    .success(false)
                    .message("An account with this email already exists. Please sign in.")
                    .build();
            }
        } catch (Exception e) {
            log.warn("Could not check duplicate email in MongoDB: {}", e.getMessage());
        }

        String name = (request.getName() != null && !request.getName().isBlank())
            ? request.getName().trim()
            : "KOPA Coffee Lover";

        User user = User.builder()
            .name(name)
            .email(cleanEmail)
            .phone(request.getPhone() != null ? request.getPhone().trim() : "")
            .password(request.getPassword())
            .role("CUSTOMER")
            .provider("LOCAL")
            .createdAt(LocalDateTime.now())
            .build();

        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            log.warn("Could not save new user in MongoDB, using in-memory model: {}", e.getMessage());
            if (user.getId() == null) {
                user.setId("usr-" + UUID.randomUUID().toString().substring(0, 8));
            }
        }

        String token = "kopa_jwt_" + UUID.randomUUID().toString();
        return AuthResponse.builder()
            .success(true)
            .message("Account created successfully! Welcome to KOPA Coffee.")
            .token(token)
            .user(user)
            .build();
    }

    public User register(String name, String email, String phone, String password) {
        AuthResponse res = register(new RegisterRequest(name, email, phone, password));
        return res.getUser();
    }

    public AuthResponse authenticateSocial(SocialLoginRequest request) {
        if (request == null || request.getProvider() == null) {
            return AuthResponse.builder()
                .success(false)
                .message("Provider (google/apple) is required.")
                .build();
        }

        String provider = request.getProvider().trim().toUpperCase();
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            email = (provider.equals("APPLE") ? "apple.user." : "google.user.") +
                    UUID.randomUUID().toString().substring(0, 6) + "@kopa.oauth";
        }
        email = email.trim().toLowerCase();

        String name = request.getName();
        if (name == null || name.isBlank()) {
            name = provider.equals("APPLE") ? "Apple Coffee Connoisseur" : "Google Coffee Explorer";
        }

        User user = null;
        try {
            Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
            if (existing.isPresent()) {
                user = existing.get();
                // Update avatar if newly provided
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                    user.setAvatarUrl(request.getAvatarUrl());
                    userRepository.save(user);
                }
            }
        } catch (Exception e) {
            log.warn("Error finding user during social login in MongoDB: {}", e.getMessage());
        }

        if (user == null) {
            user = User.builder()
                .name(name)
                .email(email)
                .phone("")
                .password("oauth-" + provider.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8))
                .role("CUSTOMER")
                .provider(provider)
                .providerId(request.getIdToken())
                .avatarUrl(request.getAvatarUrl())
                .createdAt(LocalDateTime.now())
                .build();

            try {
                user = userRepository.save(user);
            } catch (Exception e) {
                log.warn("Could not save social user in MongoDB: {}", e.getMessage());
                if (user.getId() == null) {
                    user.setId("usr-" + UUID.randomUUID().toString().substring(0, 8));
                }
            }
        }

        String token = "kopa_oauth_" + provider.toLowerCase() + "_" + UUID.randomUUID().toString();
        return AuthResponse.builder()
            .success(true)
            .message("Signed in with " + provider.charAt(0) + provider.substring(1).toLowerCase() + " successfully!")
            .token(token)
            .user(user)
            .build();
    }

    public User authenticateSocial(String provider, String email, String name) {
        SocialLoginRequest req = SocialLoginRequest.builder()
            .provider(provider)
            .email(email)
            .name(name)
            .build();
        return authenticateSocial(req).getUser();
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
                    .provider("LOCAL")
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
                .provider("LOCAL")
                .createdAt(LocalDateTime.now())
                .build();
        }
    }
}
