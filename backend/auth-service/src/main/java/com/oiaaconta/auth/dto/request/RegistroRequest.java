package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroRequest {

    @NotBlank(message = "Nome do restaurante obrigatório")
    private String nomeRestaurante;

    @NotBlank(message = "Nome do responsável obrigatório")
    private String nomeAdmin;

    @NotBlank(message = "E-mail obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    private String cnpj;
    private String telefone;
    private Long planoId;
}
