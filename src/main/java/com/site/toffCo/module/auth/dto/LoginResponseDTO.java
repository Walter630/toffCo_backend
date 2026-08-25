package com.site.toffCo.module.auth.dto;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        UserRequestDTO user
) {
    public record UserRequestDTO(String email, String name, String role) {
    }
}
