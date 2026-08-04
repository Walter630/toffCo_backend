package com.site.toffCo.module.auth.dto;

public record LoginResponseDTO(String token, String refreshToken, UserRequestDTO user) {
    public record UserRequestDTO(String email, String username, String role) {
    }
}
