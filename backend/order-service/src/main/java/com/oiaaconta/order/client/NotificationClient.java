package com.oiaaconta.order.client;

import com.oiaaconta.order.dto.NotificacaoMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/internal/notificar")
public interface NotificationClient {

    @PostMapping("/novo-pedido")
    ResponseEntity<Void> novoPedido(@RequestBody NotificacaoMessage msg);

    @PostMapping("/pedido-pronto")
    ResponseEntity<Void> pedidoPronto(@RequestBody NotificacaoMessage msg);

    @PostMapping("/pedido-entregue")
    ResponseEntity<Void> pedidoEntregue(@RequestBody NotificacaoMessage msg);
}
