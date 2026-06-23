package com.oiaaconta.notification.service;

import com.oiaaconta.notification.dto.NotificacaoMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notificarNovoPedido(NotificacaoMessage msg) {
        String destino = "/topic/cozinha/" + msg.getRestauranteId();
        log.info("Notificando cozinha: {} → {}", destino, msg.getPedidoId());
        messagingTemplate.convertAndSend(destino, msg);
    }

    public void notificarPedidoPronto(NotificacaoMessage msg) {
        String destino = "/topic/garcon/" + msg.getRestauranteId() + "/" + msg.getGarconId();
        log.info("Notificando garçom: {} → pedido {}", destino, msg.getPedidoId());
        messagingTemplate.convertAndSend(destino, msg);
    }

    public void notificarPedidoEntregue(NotificacaoMessage msg) {
        String destino = "/topic/cozinha/" + msg.getRestauranteId();
        log.info("Notificando cozinha (entregue): {} → pedido {}", destino, msg.getPedidoId());
        messagingTemplate.convertAndSend(destino, msg);
    }
}
