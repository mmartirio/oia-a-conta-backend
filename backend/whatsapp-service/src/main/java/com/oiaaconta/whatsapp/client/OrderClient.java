package com.oiaaconta.whatsapp.client;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/internal/entregas")
    EntregaResponse criarEntrega(
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestBody EntregaRequest request
    );

    // Mesmo endpoint público usado pelo cardápio público (sem X-Restaurante-Id
    // — vai por query param) pra saber se a loja está aberta antes de deixar
    // o chatbot seguir o fluxo normal (ver ChatbotService).
    @GetMapping("/api/configuracoes/pausas/status")
    StatusFuncionamentoResponse statusFuncionamento(@RequestParam("restauranteId") Long restauranteId);

    @Data
    class StatusFuncionamentoResponse {
        private boolean aberto;
        private String motivo;
        private String reaberturaPrevista;
    }

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
        private Long comboId;
        private Integer comboQuantidade;
        private List<EscolhaSaborRequest> saboresEscolhidos;
    }

    @Data
    @Builder
    class EscolhaSaborRequest {
        private Long produtoId;
        private Integer quantidade;
    }

    @Data
    class EntregaResponse {
        private Long id;
        private String status;
        private String metodoPagamento;
        private BigDecimal total;
        private BigDecimal valorFrete;
        private String pixChave;
    }
}
