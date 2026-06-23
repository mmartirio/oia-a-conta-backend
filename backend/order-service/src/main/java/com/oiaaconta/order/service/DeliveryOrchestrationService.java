package com.oiaaconta.order.service;

import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.dto.NotificacaoMessage;
import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.entity.ItemPedido;
import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusComanda;
import com.oiaaconta.order.enums.StatusEntrega;
import com.oiaaconta.order.enums.StatusPedido;
import com.oiaaconta.order.repository.ComandaRepository;
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

    private final ComandaRepository comandaRepository;
    private final PedidoRepository pedidoRepository;
    private final EntregaRepository entregaRepository;
    private final NotificationClient notificationClient;

    /**
     * Cria comanda virtual (mesa 0) + pedido na cozinha para uma entrega WhatsApp aceita.
     * Retorna o ID do pedido criado (armazenado em Entrega.pedidoCozinhaId).
     */
    @Transactional
    @SuppressWarnings("null")
    public Long criarPedidoCozinha(Entrega entrega) {
        Comanda comanda = comandaRepository.save(Comanda.builder()
            .restauranteId(entrega.getRestauranteId())
            .mesaId(0L)
            .mesaNumero(0)
            .garconId(0L)
            .garconNome("DELIVERY")
            .status(StatusComanda.ABERTA)
            .build());

        Pedido pedido = Pedido.builder()
            .comanda(comanda)
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
                .build())
            .toList();
        pedido.setItens(itens);

        Pedido saved = pedidoRepository.save(pedido);

        try {
            notificationClient.novoPedido(NotificacaoMessage.builder()
                .tipo("NOVO_PEDIDO")
                .pedidoId(saved.getId())
                .comandaId(comanda.getId())
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
}
