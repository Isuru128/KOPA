package com.kopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String name;
    
    @Indexed(unique = true)
    private String email;
    
    private String phone;
    private String password;
    private String role; // CUSTOMER, BARISTA, ADMIN

    @Builder.Default
    private String provider = "LOCAL"; // LOCAL, GOOGLE, APPLE

    private String providerId;
    private String avatarUrl;
    
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
