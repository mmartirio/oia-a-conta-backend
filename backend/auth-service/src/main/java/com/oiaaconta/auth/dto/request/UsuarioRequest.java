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

    // Opcional na edição (vazio = mantém a atual); se informada, mínimo 8.
    @jakarta.validation.constraints.Pattern(regexp = "^$|^.{8,}$", message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;

    @NotNull(message = "Role obrigatória")
    private Role role;

    // Camada adicional ao role — se informado, as permissões do grupo
    // substituem o role pra fins de autorização (ver JwtAuthFilter).
    private Long grupoId;
}
