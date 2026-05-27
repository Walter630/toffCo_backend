package com.site.toffCo.module.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "Email do usuário é obrigatório")
        @Email(message = "O email deve ter um formato válido")
        String email,

        @NotBlank(message = "A senha não pode estar em branco")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password

        ) {
}
