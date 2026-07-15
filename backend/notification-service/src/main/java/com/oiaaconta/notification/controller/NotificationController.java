package com.oiaaconta.notification.controller;

import com.oiaaconta.notification.dto.NotificacaoLocalizacaoEntrega;
import com.oiaaconta.notification.dto.NotificacaoMensagemWhatsapp;
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

    @PostMapping("/nova-entrega")
    public ResponseEntity<Void> novaEntrega(@RequestBody NotificacaoMessage msg) {
        notificationService.notificarNovaEntregaAguardando(msg);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mensagem-whatsapp")
    public ResponseEntity<Void> mensagemWhatsapp(@RequestBody NotificacaoMensagemWhatsapp msg) {
        notificationService.notificarMensagemWhatsapp(msg);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/localizacao-entrega")
    public ResponseEntity<Void> localizacaoEntrega(@RequestBody NotificacaoLocalizacaoEntrega msg) {
        notificationService.notificarLocalizacaoEntrega(msg);
        return ResponseEntity.ok().build();
    }
}
