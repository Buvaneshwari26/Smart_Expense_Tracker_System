package com.tracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String accessToken;
    @JsonAlias("accessToken")
    private String token;       // alias — both "accessToken" and "token" work in Postman scripts
    private String refreshToken;
    private String tokenType;
    private String message;
}
