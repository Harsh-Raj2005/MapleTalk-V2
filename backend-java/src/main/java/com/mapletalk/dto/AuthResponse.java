package com.mapletalk.dto;

public record AuthResponse(String token, UserResponse user) {
}
