package com.site.toffCo.module.admin.dto;

import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.user.entity.User;

import java.util.UUID;

public record AdminUserDTO(
        UUID userId,
        String userName,
        String userPhone,
        String userEmail
) {
    /**
     * Converte a entidade User para AdminUserDTO.
     */
    public static AdminUserDTO from(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "O usuário não pode ser nulo"
            );
        }

        return new AdminUserDTO(
                user.getId(),
                user.getUsername() != null
                        ? user.getUsername()
                        : "—",
                user.getPhone() != null
                        ? user.getPhone()
                        : "—",
                user.getEmail() != null ? user.getEmail() : "—"
        );
    }
}
