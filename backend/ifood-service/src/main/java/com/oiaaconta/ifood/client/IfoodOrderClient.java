package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.ifood.IfoodEventoDto;
import com.oiaaconta.ifood.dto.ifood.IfoodPedidoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Events/Order API do iFood — polling de eventos (novos pedidos, entre
// outros) e ações sobre um pedido específico. Nomes/paths conferidos pelo
// meu conhecimento geral da API, não uma consulta à doc atual.
@FeignClient(name = "ifood-order", url = "${ifood.api-url}")
public interface IfoodOrderClient {

    @GetMapping("/events/v1.0/events:polling")
    List<IfoodEventoDto> polling(@RequestHeader("Authorization") String bearerToken);

    @PostMapping("/events/v1.0/events/acknowledgment")
    void acknowledge(@RequestHeader("Authorization") String bearerToken, @RequestBody List<Map<String, String>> eventos);

    @GetMapping("/order/v1.0/orders/{orderId}")
    IfoodPedidoDto buscarPedido(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/statusChange/confirm")
    void confirmar(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/statusChange/readyToPickup")
    void prontoParaEntrega(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/statusChange/dispatch")
    void saiuParaEntrega(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/statusChange/concludeDelivery")
    void concluirEntrega(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId);

    @PostMapping("/order/v1.0/orders/{orderId}/requestCancellation")
    void cancelar(@RequestHeader("Authorization") String bearerToken, @PathVariable("orderId") String orderId, @RequestBody Map<String, String> body);
}
