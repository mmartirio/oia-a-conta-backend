package com.oiaaconta.order.service;

import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.dto.NotificacaoMessage;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.entity.ItemPedido;
import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusEntrega;
import com.oiaaconta.order.enums.StatusPedido;
import com.oiaaconta.order.repository.EntregaRepository;
import com.oiaaconta.order.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryOrchestrationService {

    private final PedidoRepository pedidoRepository;
    private final EntregaRepository entregaRepository;
    private final NotificationClient notificationClient;

    /**
     * Cria o pedido na cozinha para uma entrega (delivery) aceita — sem
     * comanda: comanda é exclusiva do fluxo de garçom/mesa. Retorna o ID do
     * pedido criado (armazenado em Entrega.pedidoCozinhaId).
     */
    @Transactional
    @SuppressWarnings("null")
    public Long criarPedidoCozinha(Entrega entrega) {
        Pedido pedido = Pedido.builder()
            .restauranteId(entrega.getRestauranteId())
            .entregaId(entrega.getId())
            .observacao(entrega.getObservacao())
            .status(StatusPedido.ENVIADO)
            .build();

        List<ItemPedido> itens = entrega.getItens().stream()
            .map(item -> ItemPedido.builder()
                .pedido(pedido)
                .produtoId(item.getProdutoId())
                .produtoNome(item.getProdutoNome())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .observacao(item.getObservacao())
                .comboId(item.getComboId())
                .comboNome(item.getComboNome())
                .build())
            .toList();
        pedido.setItens(itens);

        Pedido saved = pedidoRepository.save(pedido);

        try {
            notificationClient.novoPedido(NotificacaoMessage.builder()
                .tipo("NOVO_PEDIDO")
                .pedidoId(saved.getId())
                .restauranteId(entrega.getRestauranteId())
                .garconId(0L)
                .garconNome("DELIVERY")
                .mesaNumero(0)
                .mensagem("Novo pedido delivery: " + entrega.getClienteNome())
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar cozinha (delivery): {}", e.getMessage());
        }

        return saved.getId();
    }

    /**
     * Quando cozinha marca pedido como PRONTO, avança a Entrega vinculada para PRONTO_PARA_ENTREGA.
     */
    @Transactional
    @SuppressWarnings("null")
    public void propagarProntoParaEntrega(Pedido pedido) {
        if (pedido.getEntregaId() == null) return;
        Long entregaId = pedido.getEntregaId();
        entregaRepository.findByIdAndRestauranteId(entregaId, pedido.getRestauranteId()).ifPresent(entrega -> {
            if (entrega.getStatus() == StatusEntrega.ACEITA) {
                entrega.setStatus(StatusEntrega.PRONTO_PARA_ENTREGA);
                entregaRepository.save(entrega);
                log.info("Entrega #{} avançada para PRONTO_PARA_ENTREGA via pedido #{}", entrega.getId(), pedido.getId());
            }
        });
    }

    /**
     * Sentido inverso de propagarProntoParaEntrega: clicar "Pronto" na tela
     * de Delivery também marca o pedido vinculado como PRONTO na cozinha.
     */
    @Transactional
    public void propagarProntoParaCozinha(Entrega entrega) {
        if (entrega.getPedidoCozinhaId() == null) return;
        pedidoRepository.findByIdAndRestauranteId(entrega.getPedidoCozinhaId(), entrega.getRestauranteId())
            .ifPresent(pedido -> {
                if (pedido.getStatus() == StatusPedido.ENVIADO || pedido.getStatus() == StatusPedido.PREPARANDO) {
                    pedido.setStatus(StatusPedido.PRONTO);
                    pedido.setReadyAt(java.time.LocalDateTime.now());
                    pedidoRepository.save(pedido);
                    log.info("Pedido #{} avançado para PRONTO via entrega #{}", pedido.getId(), entrega.getId());
                }
            });
    }

    /**
     * Entregador saiu com o pedido — some da tela da cozinha (que só lista
     * ENVIADO/PREPARANDO/PRONTO).
     */
    @Transactional
    public void finalizarPedidoCozinha(Entrega entrega) {
        if (entrega.getPedidoCozinhaId() == null) return;
        pedidoRepository.findByIdAndRestauranteId(entrega.getPedidoCozinhaId(), entrega.getRestauranteId())
            .ifPresent(pedido -> {
                if (pedido.getStatus() != StatusPedido.ENTREGUE && pedido.getStatus() != StatusPedido.CANCELADO) {
                    pedido.setStatus(StatusPedido.ENTREGUE);
                    pedidoRepository.save(pedido);
                    log.info("Pedido #{} finalizado (saiu para entrega) via entrega #{}", pedido.getId(), entrega.getId());
                }
            });
    }
}
