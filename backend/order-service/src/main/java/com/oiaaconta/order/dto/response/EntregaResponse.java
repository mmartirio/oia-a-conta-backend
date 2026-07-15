package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EntregaResponse {
    private Long id;
    private Long restauranteId;

    private String clienteNome;
    private String clienteTelefone;

    private String enderecoRua;
    private String enderecoNumero;
    private String enderecoBairro;
    private String enderecoCidade;
    private String enderecoComplemento;

    private Long entregadorId;
    private String entregadorNome;

    private String status;
    private String metodoPagamento;
    private Integer parcelas;

    private String observacao;
    private String motivoRejeicao;
    private BigDecimal total;
    private List<ItemEntregaResponse> itens;

    private Long pedidoCozinhaId;
    private Boolean origemWhatsapp;
    private Boolean origemPdv;
    private Boolean pagamentoConfirmadoCaixa;
    private LocalDateTime criadoEm;
    private LocalDateTime entregueEm;

    private Double latitude;
    private Double longitude;
    private LocalDateTime localizacaoAtualizadaEm;
}
