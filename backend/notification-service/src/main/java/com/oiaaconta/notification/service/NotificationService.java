package com.oiaaconta.notification.service;

import com.oiaaconta.notification.dto.NotificacaoLocalizacaoEntrega;
import com.oiaaconta.notification.dto.NotificacaoMensagemWhatsapp;
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

    public void notificarNovaEntregaAguardando(NotificacaoMessage msg) {
        String destino = "/topic/entregas/" + msg.getRestauranteId();
        log.info("Notificando nova entrega aguardando: {} → {}", destino, msg.getPedidoId());
        messagingTemplate.convertAndSend(destino, msg);
    }

    public void notificarMensagemWhatsapp(NotificacaoMensagemWhatsapp msg) {
        String destino = "/topic/whatsapp/" + msg.getRestauranteId();
        log.info("Notificando mensagem WhatsApp: {} → {} ({})", destino, msg.getTelefone(), msg.getDirecao());
        messagingTemplate.convertAndSend(destino, msg);
    }

    public void notificarLocalizacaoEntrega(NotificacaoLocalizacaoEntrega msg) {
        String destino = "/topic/entrega-localizacao/" + msg.getRestauranteId();
        messagingTemplate.convertAndSend(destino, msg);
    }
}
