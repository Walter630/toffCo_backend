package com.site.toffCo.module.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.site.toffCo.module.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserRequestDTO(
        UUID userId,
        @NotBlank(message = "Email do usuário é obrigatório")
        @Email(message = "O email deve ter um formato válido")
        String email,

        @NotBlank(message = "A senha não pode estar em branco")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @NotBlank(message = "Numero de telefone é obrigatorio")
        String phone,
        @NotBlank(message = "Nome do usuário é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s.'-]+$", message = "Nome contém caracteres inválidos")
        @JsonAlias("name")
        String username,

        Role role
) {
}
