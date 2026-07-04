package com.site.toffCo.module.user.dto;

import java.util.UUID;

public record UserResponseDTO(
        UUID userId,
        String email,
        String username
) {}
