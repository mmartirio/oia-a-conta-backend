package com.oiaaconta.order.service;

import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.dto.NotificacaoMessage;
import com.oiaaconta.order.dto.request.PedidoRequest;
import com.oiaaconta.order.dto.response.ItemPedidoResponse;
import com.oiaaconta.order.dto.response.PedidoResponse;
import com.oiaaconta.order.dto.response.ResumoDiaResponse;
import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.entity.ItemPedido;
import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusComanda;
import com.oiaaconta.order.enums.StatusPedido;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.exception.ResourceNotFoundException;
import com.oiaaconta.order.repository.ComandaRepository;
import com.oiaaconta.order.repository.PedidoRepository;
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
    private final DeliveryOrchestrationService orchestrationService;
    private final FinanceiroService financeiroService;

    @Transactional
    public PedidoResponse enviarParaCozinha(Long restauranteId, Long comandaId, PedidoRequest request) {
        Comanda comanda = comandaRepository.findByIdAndRestauranteId(comandaId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        if (comanda.getStatus() != StatusComanda.ABERTA) {
            throw new BusinessException("Comanda está fechada");
        }

        StatusPedido statusInicial = request.isCozinha() ? StatusPedido.ENVIADO : StatusPedido.ENTREGUE;

        Pedido pedido = Pedido.builder()
            .comanda(comanda)
            .restauranteId(restauranteId)
            .observacao(request.getObservacao())
            .status(statusInicial)
            .build();

        List<ItemPedido> itens = request.getItens().stream()
            .map(item -> ItemPedido.builder()
                .pedido(pedido)
                .produtoId(item.getProdutoId())
                .produtoNome(item.getProdutoNome() != null ? item.getProdutoNome() : "Produto #" + item.getProdutoId())
                .quantidade(item.getQuantidade())
                .observacao(item.getObservacao())
                .precoUnitario(item.getPrecoUnitario() != null ? item.getPrecoUnitario() : BigDecimal.ZERO)
                .build())
            .toList();
        pedido.setItens(itens);

        Pedido saved = pedidoRepository.save(pedido);

        if (request.isCozinha()) {
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
        }

        return toResponse(saved);
    }

    public List<PedidoResponse> listarAtivos(Long restauranteId) {
        return pedidoRepository.findByRestauranteIdAndStatusInOrderByCreatedAtAsc(
            restauranteId, List.of(StatusPedido.ENVIADO, StatusPedido.PREPARANDO, StatusPedido.PRONTO))
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PedidoResponse marcarPreparando(Long restauranteId, Long id, Long cozinheiroId, String cozinheiroNome) {
        Pedido pedido = findPedido(restauranteId, id);
        pedido.setStatus(StatusPedido.PREPARANDO);
        if (cozinheiroId != null) pedido.setCozinheiroId(cozinheiroId);
        if (cozinheiroNome != null) pedido.setCozinheiroNome(cozinheiroNome);
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse marcarPronto(Long restauranteId, Long id) {
        Pedido pedido = findPedido(restauranteId, id);
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.setReadyAt(LocalDateTime.now());
        Pedido saved = pedidoRepository.save(pedido);

        try {
            orchestrationService.propagarProntoParaEntrega(saved);
        } catch (Exception e) {
            log.warn("Falha ao propagar PRONTO_PARA_ENTREGA: {}", e.getMessage());
        }

        try {
            // Para pedidos de mesa (não delivery virtual), notifica o garçom
            if (saved.getComanda().getMesaNumero() != 0) {
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
            }
        } catch (Exception e) {
            log.warn("Falha ao notificar garçom: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public PedidoResponse marcarEntregue(Long restauranteId, Long id) {
        Pedido pedido = findPedido(restauranteId, id);
        pedido.setStatus(StatusPedido.ENTREGUE);
        Pedido saved = pedidoRepository.save(pedido);

        try {
            notificationClient.pedidoEntregue(NotificacaoMessage.builder()
                .tipo("PEDIDO_ENTREGUE")
                .pedidoId(saved.getId())
                .comandaId(saved.getComanda().getId())
                .restauranteId(restauranteId)
                .mesaNumero(saved.getComanda().getMesaNumero())
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar entrega: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public PedidoResponse cancelar(Long restauranteId, Long id) {
        Pedido pedido = findPedido(restauranteId, id);
        if (pedido.getStatus() != StatusPedido.ENVIADO && pedido.getStatus() != StatusPedido.PREPARANDO) {
            throw new BusinessException("Pedido não pode mais ser cancelado");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        return toResponse(pedidoRepository.save(pedido));
    }

    public ResumoDiaResponse getResumoDia(Long restauranteId) {
        LocalDateTime inicio = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime fim = LocalDateTime.now();

        long total = pedidoRepository.countByRestauranteIdAndCreatedAtBetween(restauranteId, inicio, fim);
        long emProducao = pedidoRepository.countByRestauranteIdAndStatusInAndCreatedAtBetween(
            restauranteId, List.of(StatusPedido.ENVIADO, StatusPedido.PREPARANDO, StatusPedido.PRONTO), inicio, fim);
        long cancelados = pedidoRepository.countByRestauranteIdAndStatusInAndCreatedAtBetween(
            restauranteId, List.of(StatusPedido.CANCELADO), inicio, fim);
        long entregues = pedidoRepository.countByRestauranteIdAndStatusInAndCreatedAtBetween(
            restauranteId, List.of(StatusPedido.ENTREGUE), inicio, fim);

        // Reaproveita o mesmo cálculo de receita confirmada do Financeiro
        // (comandas fechadas + entregas com pagamento confirmado no caixa).
        var resumoFinanceiro = financeiroService.getResumo(restauranteId, inicio, fim);
        BigDecimal totalCaixa = resumoFinanceiro.getTotalGeral();
        long qtdVendas = resumoFinanceiro.getQtdComandas() + resumoFinanceiro.getQtdEntregas();
        BigDecimal ticketMedio = qtdVendas > 0
            ? totalCaixa.divide(BigDecimal.valueOf(qtdVendas), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ResumoDiaResponse.builder()
            .total(total)
            .emProducao(emProducao)
            .cancelados(cancelados)
            .entregues(entregues)
            .totalCaixa(totalCaixa)
            .ticketMedio(ticketMedio)
            .build();
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
            .itens(itens).criadoEm(p.getCreatedAt()).prontoEm(p.getReadyAt())
            .build();
    }
}
