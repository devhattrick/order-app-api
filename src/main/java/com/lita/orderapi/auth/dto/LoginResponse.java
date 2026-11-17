package com.lita.orderapi.auth.dto;

public record LoginResponse(
        String token,
        long expiresInSeconds,
        UserProfile user
) {
}
