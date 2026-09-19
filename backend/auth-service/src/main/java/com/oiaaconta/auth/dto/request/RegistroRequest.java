package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
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

    // Só relevante quando o plano escolhido exige modalidade de operação
    // (ex: plano Startup) — validado no billing-service ao criar o contrato.
    private String modalidadeOperacao;

    // Aceite do Contrato de Adesão/Termos de Uso/Política de Privacidade —
    // @AssertTrue rejeita a requisição (400) se vier false, então o registro
    // não avança sem o aceite mesmo que alguém pule o checkbox do front.
    @AssertTrue(message = "É necessário aceitar o Contrato de Adesão, os Termos de Uso e a Política de Privacidade")
    private boolean termosAceitos;

    @NotBlank(message = "Versão do contrato obrigatória")
    private String versaoContrato;
}
