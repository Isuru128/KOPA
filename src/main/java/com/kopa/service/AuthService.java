package com.kopa.service;

import com.kopa.dto.AuthRequest;
import com.kopa.dto.AuthResponse;
import com.kopa.dto.RegisterRequest;
import com.kopa.dto.SocialLoginRequest;
import com.kopa.model.User;
import com.kopa.repository.UserRepository;
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
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Authenticate real registered user with email and password
     */
    public AuthResponse login(AuthRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank() 
            || request.getPassword() == null || request.getPassword().isBlank()) {
            return AuthResponse.builder()
                .success(false)
                .message("Please provide both email address and password.")
                .build();
        }

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        try {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getPassword() != null && user.getPassword().equals(password)) {
                    String token = jwtService.generateToken(user);
                    return AuthResponse.builder()
                        .success(true)
                        .message("Welcome back, " + user.getName() + "!")
                        .token(token)
                        .user(user)
                        .build();
                }
            }
        } catch (Exception e) {
            log.error("Database error during user login: {}", e.getMessage());
        }

        return AuthResponse.builder()
            .success(false)
            .message("Invalid email or password. Please check your credentials or create a new account.")
            .build();
    }

    public Optional<User> login(String email, String password) {
        AuthResponse res = login(new AuthRequest(email, password));
        return res.isSuccess() ? Optional.ofNullable(res.getUser()) : Optional.empty();
    }

    /**
     * Register a new real user account and persist into MongoDB
     */
    public AuthResponse register(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
            || request.getName() == null || request.getName().isBlank()
            || request.getPassword() == null || request.getPassword().isBlank()) {
            return AuthResponse.builder()
                .success(false)
                .message("Full name, valid email address and password are required.")
                .build();
        }

        if (request.getPassword().length() < 4) {
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
                    .message("An account with this email address is already registered. Please sign in.")
                    .build();
            }
        } catch (Exception e) {
            log.warn("Could not check duplicate email in MongoDB: {}", e.getMessage());
        }

        User newUser = User.builder()
            .name(request.getName().trim())
            .email(cleanEmail)
            .phone(request.getPhone() != null ? request.getPhone().trim() : "")
            .password(request.getPassword())
            .role("CUSTOMER")
            .provider("LOCAL")
            .createdAt(LocalDateTime.now())
            .build();

        try {
            newUser = userRepository.save(newUser);
        } catch (Exception e) {
            log.error("Failed to save new user to database: {}", e.getMessage());
            return AuthResponse.builder()
                .success(false)
                .message("Database error while registering account. Please try again.")
                .build();
        }

        String token = jwtService.generateToken(newUser);
        return AuthResponse.builder()
            .success(true)
            .message("Welcome to KOPA Coffee, " + newUser.getName() + "! Your account is active.")
            .token(token)
            .user(newUser)
            .build();
    }

    public User register(String name, String email, String phone, String password) {
        AuthResponse res = register(new RegisterRequest(name, email, phone, password));
        return res.getUser();
    }

    /**
     * Real Social Login / OAuth account provisioner
     */
    public AuthResponse authenticateSocial(SocialLoginRequest request) {
        if (request == null || request.getProvider() == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return AuthResponse.builder()
                .success(false)
                .message("Valid social provider and email are required.")
                .build();
        }

        String provider = request.getProvider().trim().toUpperCase();
        String cleanEmail = request.getEmail().trim().toLowerCase();
        String name = (request.getName() != null && !request.getName().isBlank())
            ? request.getName().trim()
            : (provider.equals("APPLE") ? "Apple Coffee Member" : "Google Coffee Member");

        User user = null;
        try {
            Optional<User> existing = userRepository.findByEmailIgnoreCase(cleanEmail);
            if (existing.isPresent()) {
                user = existing.get();
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                    user.setAvatarUrl(request.getAvatarUrl());
                    userRepository.save(user);
                }
            }
        } catch (Exception e) {
            log.warn("Database error during social user lookup: {}", e.getMessage());
        }

        if (user == null) {
            user = User.builder()
                .name(name)
                .email(cleanEmail)
                .phone("")
                .password("oauth-" + provider.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 10))
                .role("CUSTOMER")
                .provider(provider)
                .providerId(request.getIdToken())
                .avatarUrl(request.getAvatarUrl())
                .createdAt(LocalDateTime.now())
                .build();

            try {
                user = userRepository.save(user);
            } catch (Exception e) {
                log.error("Failed to persist social user in database: {}", e.getMessage());
                return AuthResponse.builder()
                    .success(false)
                    .message("Failed to create social account in database.")
                    .build();
            }
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
            .success(true)
            .message("Authenticated with " + provider.charAt(0) + provider.substring(1).toLowerCase() + " successfully!")
            .token(token)
            .user(user)
            .build();
    }

    public Optional<User> getUserByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public Optional<User> getUserFromJwtToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        if (!jwtService.isTokenValid(token)) return Optional.empty();
        String email = jwtService.extractEmail(token);
        return getUserByEmail(email);
    }
}
