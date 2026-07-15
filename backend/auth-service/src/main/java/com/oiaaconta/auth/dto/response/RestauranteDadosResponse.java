package com.oiaaconta.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestauranteDadosResponse {
    private String nome;
    private String slug;
    private String plano;
    private boolean ativo;
    private String cnpj;
    private String telefone;
    private String enderecoRua;
    private String enderecoNumero;
    private String enderecoBairro;
    private String enderecoCidade;
    private String enderecoComplemento;
    private String logoBase64;
    private String corPrimaria;
    private String corSecundaria;
    private String corAccent;
    private String corTexto;
    private String backgroundBase64;
    private Integer backgroundOpacidade;
    private Double latitude;
    private Double longitude;
}
