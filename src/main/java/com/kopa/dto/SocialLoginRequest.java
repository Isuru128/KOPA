package com.kopa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {
    private String provider; // "google" or "apple"
    private String idToken;
    private String email;
    private String name;
    private String avatarUrl;
}
