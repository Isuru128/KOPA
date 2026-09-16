package com.kopa.controller;

import com.kopa.model.User;
import com.kopa.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> creds) {
        String email = creds.get("email");
        String password = creds.get("password");

        return authService.login(email, password)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password. Use demo account or create a new profile.")));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");
        String phone = payload.get("phone");
        String password = payload.get("password");

        User user = authService.register(name, email, phone, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/demo")
    public ResponseEntity<User> getDemoUser() {
        return ResponseEntity.ok(authService.getDemoUser());
    }

    @PostMapping("/social")
    public ResponseEntity<User> socialAuth(@RequestBody Map<String, String> payload) {
        String provider = payload.getOrDefault("provider", "google");
        String email = payload.get("email");
        String name = payload.get("name");
        User user = authService.authenticateSocial(provider, email, name);
        return ResponseEntity.ok(user);
    }
}
