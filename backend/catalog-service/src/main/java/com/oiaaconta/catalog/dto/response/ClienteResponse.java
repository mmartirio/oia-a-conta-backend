package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ClienteResponse {
    private Long id;
    private Long restauranteId;
    private String nome;
    private String telefone;
    private String email;
    private LocalDate dataNascimento;
    private String enderecoRua;
    private String enderecoNumero;
    private String enderecoBairro;
    private String enderecoCidade;
    private String enderecoComplemento;
    private String enderecoCep;
    private String observacoes;
    private boolean ativo;
    private LocalDateTime createdAt;
}
