package com.oiaaconta.auth.dto.request;

import com.oiaaconta.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsuarioRequest {

    @NotBlank(message = "Nome obrigatório")
    private String nome;

    @NotBlank(message = "E-mail obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    private String senha;

    @NotNull(message = "Role obrigatória")
    private Role role;
}
