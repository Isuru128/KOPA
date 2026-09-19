package com.kopa.service;

import com.kopa.model.User;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String DEFAULT_SECRET = "kopaCoffeeSpecialtyRoastersSecretKeyForJwtAuthenticationMustBeAtLeast256BitsLong2026SecureKey!";
    private static final long DEFAULT_EXPIRATION_MS = 86400000L * 7; // 7 days

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService() {
        String secret = DEFAULT_SECRET;
        long exp = DEFAULT_EXPIRATION_MS;

        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String envSecret = dotenv.get("JWT_SECRET");
            if (envSecret != null && !envSecret.isBlank()) {
                secret = envSecret;
            }
            String envExp = dotenv.get("JWT_EXPIRATION_MS");
            if (envExp != null && !envExp.isBlank()) {
                exp = Long.parseLong(envExp);
            }
        } catch (Exception e) {
            log.warn("Using default JWT configuration: {}", e.getMessage());
        }

        // Ensure key is sufficiently long for HMAC-SHA256 (at least 32 bytes)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            this.signingKey = Keys.hmacShaKeyFor(padded);
        } else {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }

        this.expirationMs = exp;
        log.info("JwtService initialized with token validity of {} ms", expirationMs);
    }

    /**
     * Generate signed JWT token with user details and claims
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (user.getId() != null) extraClaims.put("userId", user.getId());
        if (user.getName() != null) extraClaims.put("name", user.getName());
        if (user.getRole() != null) extraClaims.put("role", user.getRole());
        if (user.getProvider() != null) extraClaims.put("provider", user.getProvider());
        if (user.getAvatarUrl() != null) extraClaims.put("avatarUrl", user.getAvatarUrl());

        return generateToken(extraClaims, user.getEmail());
    }

    public String generateToken(Map<String, Object> extraClaims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .claims(extraClaims)
            .subject(subject)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMs))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Extract email/subject from JWT token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract single claim using resolver function
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    /**
     * Validate token integrity and check if expired
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims == null) return false;
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        final String email = extractEmail(token);
        return (email != null && email.equalsIgnoreCase(expectedEmail) && isTokenValid(token));
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.warn("Could not parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }
}
