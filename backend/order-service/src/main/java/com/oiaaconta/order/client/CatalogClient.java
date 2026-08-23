package com.oiaaconta.order.client;

import com.oiaaconta.order.dto.catalog.ComboResponseDto;
import com.oiaaconta.order.dto.catalog.CupomValidacaoDto;
import com.oiaaconta.order.dto.catalog.EstoqueBaixaRequestDto;
import com.oiaaconta.order.dto.catalog.PromocaoAplicavelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// Chamadas síncronas order-service -> catalog-service no momento da venda:
// baixa/liberação de estoque, expansão de combo e validação de cupom/promoção.
// Diferente do TableClient (efeito colateral best-effort), a baixa de estoque
// NÃO pode ser engolida em try/catch pelos call-sites — é regra de negócio
// dura ("não vender o que não tem").
@FeignClient(name = "catalog-service", path = "/api")
public interface CatalogClient {

    @PostMapping("/estoque/verificar-baixar")
    void verificarEBaixarEstoque(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestBody EstoqueBaixaRequestDto request);

    @PostMapping("/estoque/liberar")
    void liberarEstoque(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestBody EstoqueBaixaRequestDto request);

    @GetMapping("/combos/{id}")
    ComboResponseDto buscarCombo(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @PathVariable("id") Long id);

    // Variante pública (sem Authorization) — usada pelo fluxo de pedido do
    // cardápio público/WhatsApp, que não tem JWT de usuário (cliente anônimo).
    // Mesmo dado do endpoint acima, só que exposto sem exigir autenticação.
    @GetMapping("/catalog/publico/{restauranteId}/combos/{id}")
    ComboResponseDto buscarComboPublico(
        @PathVariable("restauranteId") Long restauranteId,
        @PathVariable("id") Long id);

    // Elegibilidade do cupom (alvo + validade) não depende do valor da compra
    // — só do gasto histórico (usado em promocoesAplicaveis). O valor do
    // desconto em si é calculado localmente em ComandaService a partir do
    // subtotal, que já é conhecido lá.
    @GetMapping("/cupons/validar")
    CupomValidacaoDto validarCupom(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestParam("codigo") String codigo,
        @RequestParam(value = "clienteId", required = false) Long clienteId);

    @GetMapping("/promocoes/aplicaveis")
    List<PromocaoAplicavelDto> promocoesAplicaveis(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestParam(value = "clienteId", required = false) Long clienteId,
        @RequestParam(value = "gastoHistorico", required = false) BigDecimal gastoHistorico);
}
