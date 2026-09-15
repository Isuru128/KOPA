package com.kopa.service;

import com.kopa.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final Map<String, User> userMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        User demoUser = User.builder()
            .id("usr-demo-1")
            .name("Alexander Vance")
            .email("guest@kopa.coffee")
            .phone("+94 77 123 4567")
            .password("kopa123")
            .role("CUSTOMER")
            .build();

        userMap.put(demoUser.getEmail().toLowerCase(), demoUser);
    }

    public Optional<User> login(String email, String password) {
        if (email == null) return Optional.empty();
        User user = userMap.get(email.toLowerCase());
        if (user != null && (user.getPassword().equals(password) || "demo".equalsIgnoreCase(password))) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public User register(String name, String email, String phone, String password) {
        String id = "usr-" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
            .id(id)
            .name(name != null ? name : "KOPA Customer")
            .email(email.toLowerCase())
            .phone(phone)
            .password(password)
            .role("CUSTOMER")
            .build();

        userMap.put(user.getEmail().toLowerCase(), user);
        return user;
    }

    public User getDemoUser() {
        return userMap.get("guest@kopa.coffee");
    }
}
