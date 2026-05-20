package com.site.toffCo.module.user.dto;

import com.site.toffCo.module.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotBlank(message = "Email do usuário é obrigatório")
        @Email(message = "O email deve ter um formato válido")
        String email,

        @NotBlank(message = "A senha não pode estar em branco")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @NotBlank(message = "Numero de telefone é obrigatorio")
        String phone,

        String name,

        Role role // Mudei para singular, já que geralmente é um Role por vez, ou Set<Role> se forem vários
) {
}
