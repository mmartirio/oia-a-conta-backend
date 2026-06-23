package com.oiaaconta.notification.controller;

import com.oiaaconta.notification.dto.NotificacaoMessage;
import com.oiaaconta.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/notificar")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/novo-pedido")
    public ResponseEntity<Void> novoPedido(@RequestBody NotificacaoMessage msg) {
        notificationService.notificarNovoPedido(msg);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pedido-pronto")
    public ResponseEntity<Void> pedidoPronto(@RequestBody NotificacaoMessage msg) {
        notificationService.notificarPedidoPronto(msg);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pedido-entregue")
    public ResponseEntity<Void> pedidoEntregue(@RequestBody NotificacaoMessage msg) {
        notificationService.notificarPedidoEntregue(msg);
        return ResponseEntity.ok().build();
    }
}
