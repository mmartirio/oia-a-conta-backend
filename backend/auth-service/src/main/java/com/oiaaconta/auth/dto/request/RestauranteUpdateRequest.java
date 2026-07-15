package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RestauranteUpdateRequest {

    @Pattern(regexp = "\\s*\\S.*", message = "Nome não pode ser vazio")
    private String nome;

    private String slug;

    private String cnpj;

    private String telefone;

    private String enderecoRua;

    private String enderecoNumero;

    private String enderecoBairro;

    private String enderecoCidade;

    private String enderecoComplemento;
}
