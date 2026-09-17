package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.ifood.IfoodEventoDto;
import com.oiaaconta.ifood.dto.ifood.IfoodPedidoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Events/Order API do iFood — polling de eventos (novos pedidos, entre
// outros) e ações sobre um pedido específico.
//
// Paths de statusChange corrigidos em 2026-09 — a doc oficial
// (developer.ifood.com.br/docs/guides/modules/order/endpoints) não usa
// prefixo "/statusChange/": é POST direto em
// /order/v1.0/orders/{id}/<ação> (confirm, startPreparation, readyToPickup,
// dispatch, requestCancellation). A versão anterior tinha um segmento
// "/statusChange/" a mais em confirm/readyToPickup/dispatch (404 na API
// real — o pedido nunca era confirmado e o iFood cancelava sozinho depois
// de ~8min) e chamava um "/statusChange/concludeDelivery" que não existe:
// o evento CONCLUDED é gerado automaticamente pelo iFood, sem endpoint
// manual pra isso (ver IfoodInternalController).
@FeignClient(name = "ifood-order", url = "${ifood.api-url}")
public interface IfoodOrderClient {

    @GetMapping("/events/v1.0/events:polling")
    List<IfoodEventoDto> polling(@RequestHeader("Authorization") String bearerToken);

    @PostMapping("/events/v1.0/events/acknowledgment")
    void acknowledge(@RequestHeader("Authorization") String bearerToken, @RequestBody List<Map<String, String>> eventos);

    @GetMapping("/order/v1.0/orders/{orderId}")
    IfoodPedidoDto buscarPedido(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/confirm")
    void confirmar(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    // Opcional pra a API do iFood (não bloqueia o fluxo se falhar), só
    // recomendado pra melhor experiência do cliente — chamado best-effort
    // logo após confirmar().
    @PostMapping("/order/v1.0/orders/{orderId}/startPreparation")
    void iniciarPreparo(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/readyToPickup")
    void prontoParaEntrega(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/dispatch")
    void saiuParaEntrega(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/requestCancellation")
    void cancelar(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId, @RequestBody Map<String, String> body);
}
