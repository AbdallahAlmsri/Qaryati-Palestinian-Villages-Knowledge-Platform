package com.qaryati.qaryati.auth;

public record AuthResponse(
        String token,
        String username,
        String role
) {
}