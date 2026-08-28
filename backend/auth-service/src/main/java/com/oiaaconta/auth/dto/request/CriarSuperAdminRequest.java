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

    private String senha;
}
