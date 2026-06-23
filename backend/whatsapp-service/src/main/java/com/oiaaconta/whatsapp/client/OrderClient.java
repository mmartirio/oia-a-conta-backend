package com.oiaaconta.whatsapp.client;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/api/entregas")
    EntregaResponse criarEntrega(
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestBody EntregaRequest request
    );

    @Data
    @Builder
    class EntregaRequest {
        private String clienteNome;
        private String clienteTelefone;
        private String enderecoRua;
        private String enderecoNumero;
        private String enderecoBairro;
        private String enderecoCidade;
        private String enderecoComplemento;
        private String metodoPagamento;
        private String observacao;
        private boolean origemWhatsapp;
        private List<ItemEntregaRequest> itens;
    }

    @Data
    @Builder
    class ItemEntregaRequest {
        private Long produtoId;
        private String produtoNome;
        private BigDecimal precoUnitario;
        private Integer quantidade;
    }

    @Data
    class EntregaResponse {
        private Long id;
        private String status;
        private String metodoPagamento;
        private BigDecimal total;
    }
}
