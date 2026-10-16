package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.order.StatusFuncionamentoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

// Só pode existir UM @FeignClient(name = "order-service") no contexto —
// duas interfaces separadas com o mesmo "name" batem no registro do bean
// FeignClientSpecification (Spring recusa a subir). Por isso as duas
// chamadas a order-service (injetar entrega + consultar status da loja)
// ficam juntas aqui, não em clients separados.
@FeignClient(name = "order-service")
public interface OrderClient {

    // /internal/entregas — mesmo endpoint que whatsapp-service já usa pra
    // injetar pedidos, sem JWT, só acessível dentro da rede Docker.
    @PostMapping("/internal/entregas")
    EntregaResponse criarEntrega(@RequestHeader("X-Restaurante-Id") Long restauranteId, @RequestBody EntregaRequest request);

    // Cancelamento decidido do lado do iFood (evento CANCELLED via polling)
    // — atualiza o status local sem reabrir um novo ciclo de notificação.
    @PostMapping("/internal/entregas/ifood/{ifoodOrderId}/cancelar")
    void cancelarEntregaIfood(@RequestHeader("X-Restaurante-Id") Long restauranteId, @PathVariable("ifoodOrderId") String ifoodOrderId);

    // Conclusão decidida do lado do iFood (evento CONCLUDED via polling) —
    // gerado automaticamente por eles depois da entrega, principalmente em
    // envio imediato.
    @PostMapping("/internal/entregas/ifood/{ifoodOrderId}/concluir")
    void concluirEntregaIfood(@RequestHeader("X-Restaurante-Id") Long restauranteId, @PathVariable("ifoodOrderId") String ifoodOrderId);

    // Endpoint público (sem autenticação) — usado pela sincronização de
    // status da loja (ver IfoodStatusSyncService).
    @GetMapping("/api/configuracoes/pausas/status")
    StatusFuncionamentoDto statusPausa(@RequestParam("restauranteId") Long restauranteId);

    class EntregaRequest {
        public String clienteNome;
        public String clienteTelefone;
        public String enderecoRua;
        public String enderecoNumero;
        public String enderecoBairro;
        public String enderecoCidade;
        public String enderecoComplemento;
        public String metodoPagamento;
        public String observacao;
        public boolean origemIfood;
        public String ifoodOrderId;
        public String ifoodDisplayId;
        public List<ItemEntregaRequest> itens;
    }

    class ItemEntregaRequest {
        public Long produtoId;
        public String produtoNome;
        public BigDecimal precoUnitario;
        public Integer quantidade;
        public Long comboId;
        public Integer comboQuantidade;
    }

    class EntregaResponse {
        public Long id;
        public String status;
        public String metodoPagamento;
        public BigDecimal total;
    }
}
