package com.comandadigital.order.service;

import com.comandadigital.order.client.NotificationClient;
import com.comandadigital.order.dto.NotificacaoMessage;
import com.comandadigital.order.dto.request.PedidoRequest;
import com.comandadigital.order.dto.response.ComandaResponse;
import com.comandadigital.order.dto.response.ItemPedidoResponse;
import com.comandadigital.order.dto.response.PedidoResponse;
import com.comandadigital.order.entity.Comanda;
import com.comandadigital.order.entity.ItemPedido;
import com.comandadigital.order.entity.Pedido;
import com.comandadigital.order.enums.StatusComanda;
import com.comandadigital.order.enums.StatusPedido;
import com.comandadigital.order.exception.BusinessException;
import com.comandadigital.order.exception.ResourceNotFoundException;
import com.comandadigital.order.repository.ComandaRepository;
import com.comandadigital.order.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ComandaRepository comandaRepository;
    private final NotificationClient notificationClient;

    @Transactional
    public PedidoResponse enviarParaCozinha(Long restauranteId, Long comandaId, PedidoRequest request) {
        Comanda comanda = comandaRepository.findByIdAndRestauranteId(comandaId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        if (comanda.getStatus() != StatusComanda.ABERTA) {
            throw new BusinessException("Comanda está fechada");
        }

        Pedido pedido = Pedido.builder()
            .comanda(comanda)
            .restauranteId(restauranteId)
            .observacao(request.getObservacao())
            .status(StatusPedido.ENVIADO)
            .build();

        List<ItemPedido> itens = request.getItens().stream()
            .map(item -> ItemPedido.builder()
                .pedido(pedido)
                .produtoId(item.getProdutoId())
                .produtoNome("Produto #" + item.getProdutoId())
                .quantidade(item.getQuantidade())
                .observacao(item.getObservacao())
                .precoUnitario(BigDecimal.ZERO)
                .build())
            .toList();
        pedido.setItens(itens);

        Pedido saved = pedidoRepository.save(pedido);

        try {
            notificationClient.novoPedido(NotificacaoMessage.builder()
                .tipo("NOVO_PEDIDO")
                .pedidoId(saved.getId())
                .comandaId(comandaId)
                .restauranteId(restauranteId)
                .garconId(comanda.getGarconId())
                .garconNome(comanda.getGarconNome())
                .mesaNumero(comanda.getMesaNumero())
                .mensagem("Novo pedido da mesa " + comanda.getMesaNumero())
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar cozinha: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    public List<PedidoResponse> listarAtivos(Long restauranteId) {
        return pedidoRepository.findByRestauranteIdAndStatusInOrderByCreatedAtAsc(
            restauranteId, List.of(StatusPedido.ENVIADO, StatusPedido.PREPARANDO, StatusPedido.PRONTO))
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PedidoResponse marcarPreparando(Long restauranteId, Long id) {
        return atualizarStatus(restauranteId, id, StatusPedido.PREPARANDO, null);
    }

    @Transactional
    public PedidoResponse marcarPronto(Long restauranteId, Long id) {
        Pedido pedido = findPedido(restauranteId, id);
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.setReadyAt(LocalDateTime.now());
        Pedido saved = pedidoRepository.save(pedido);

        try {
            notificationClient.pedidoPronto(NotificacaoMessage.builder()
                .tipo("PEDIDO_PRONTO")
                .pedidoId(saved.getId())
                .comandaId(saved.getComanda().getId())
                .restauranteId(restauranteId)
                .garconId(saved.getComanda().getGarconId())
                .garconNome(saved.getComanda().getGarconNome())
                .mesaNumero(saved.getComanda().getMesaNumero())
                .mensagem("Pedido pronto! Mesa " + saved.getComanda().getMesaNumero())
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar garçom: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public PedidoResponse marcarEntregue(Long restauranteId, Long id) {
        return atualizarStatus(restauranteId, id, StatusPedido.ENTREGUE, null);
    }

    private PedidoResponse atualizarStatus(Long restauranteId, Long id,
                                             StatusPedido status, LocalDateTime readyAt) {
        Pedido pedido = findPedido(restauranteId, id);
        pedido.setStatus(status);
        if (readyAt != null) pedido.setReadyAt(readyAt);
        return toResponse(pedidoRepository.save(pedido));
    }

    private Pedido findPedido(Long restauranteId, Long id) {
        return pedidoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
    }

    PedidoResponse toResponse(Pedido p) {
        List<ItemPedidoResponse> itens = p.getItens().stream()
            .map(i -> ItemPedidoResponse.builder()
                .id(i.getId()).produtoId(i.getProdutoId())
                .produtoNome(i.getProdutoNome()).quantidade(i.getQuantidade())
                .observacao(i.getObservacao()).precoUnitario(i.getPrecoUnitario())
                .subtotal(i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .build())
            .toList();
        return PedidoResponse.builder()
            .id(p.getId()).comandaId(p.getComanda().getId())
            .restauranteId(p.getRestauranteId())
            .mesaNumero(p.getComanda().getMesaNumero())
            .garconId(p.getComanda().getGarconId())
            .garconNome(p.getComanda().getGarconNome())
            .status(p.getStatus().name()).observacao(p.getObservacao())
            .itens(itens).createdAt(p.getCreatedAt()).readyAt(p.getReadyAt())
            .build();
    }
}
