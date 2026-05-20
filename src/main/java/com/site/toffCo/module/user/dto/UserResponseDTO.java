package com.site.toffCo.module.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO(
        String email,
        String name
) {}
