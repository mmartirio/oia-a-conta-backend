package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClienteRequest {
    @NotBlank(message = "Nome do cliente obrigatório")
    private String nome;

    @NotBlank(message = "Telefone obrigatório")
    private String telefone;

    @Email(message = "E-mail inválido")
    private String email;

    private LocalDate dataNascimento;

    private String enderecoRua;
    private String enderecoNumero;
    private String enderecoBairro;
    private String enderecoCidade;
    private String enderecoComplemento;
    private String enderecoCep;

    private String observacoes;
}
