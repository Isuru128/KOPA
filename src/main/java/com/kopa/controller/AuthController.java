package com.kopa.controller;

import com.kopa.dto.AuthRequest;
import com.kopa.dto.AuthResponse;
import com.kopa.dto.RegisterRequest;
import com.kopa.dto.SocialLoginRequest;
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
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/social")
    public ResponseEntity<AuthResponse> socialAuth(@RequestBody SocialLoginRequest request) {
        AuthResponse response = authService.authenticateSocial(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/demo")
    public ResponseEntity<User> getDemoUser() {
        return ResponseEntity.ok(authService.getDemoUser());
    }
}
