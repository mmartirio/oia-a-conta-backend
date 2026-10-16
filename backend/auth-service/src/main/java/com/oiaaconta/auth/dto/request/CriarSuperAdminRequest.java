package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CriarSuperAdminRequest {

    @NotBlank(message = "Nome obrigatório")
    private String nome;

    @NotBlank(message = "E-mail obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    // Opcional na edição (vazio = mantém a atual); se informada, mínimo 12.
    @jakarta.validation.constraints.Pattern(regexp = "^$|^.{12,}$", message = "Senha de SUPER_ADMIN deve ter no mínimo 12 caracteres")
    private String senha;
}
