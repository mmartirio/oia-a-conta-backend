package com.oiaaconta.order.service;

import com.oiaaconta.order.client.CatalogClient;
import com.oiaaconta.order.client.CatalogClientErrorUtil;
import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.dto.NotificacaoMessage;
import com.oiaaconta.order.dto.catalog.ComboItemDto;
import com.oiaaconta.order.dto.catalog.ComboResponseDto;
import com.oiaaconta.order.dto.catalog.EstoqueBaixaRequestDto;
import com.oiaaconta.order.dto.catalog.ItemQuantidadeDto;
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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final CatalogClient catalogClient;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PedidoResponse enviarParaCozinha(Long restauranteId, Long comandaId, PedidoRequest request, String authHeader) {
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

        List<ItemPedido> itens = new ArrayList<>();
        for (PedidoRequest.ItemRequest item : request.getItens()) {
            if (item.getComboId() != null) {
                itens.addAll(expandirCombo(pedido, item, restauranteId, authHeader));
            } else if (item.getProdutoId() != null) {
                itens.add(ItemPedido.builder()
                    .pedido(pedido)
                    .produtoId(item.getProdutoId())
                    .produtoNome(item.getProdutoNome() != null ? item.getProdutoNome() : "Produto #" + item.getProdutoId())
                    .quantidade(item.getQuantidade())
                    .observacao(item.getObservacao())
                    .precoUnitario(item.getPrecoUnitario() != null ? item.getPrecoUnitario() : BigDecimal.ZERO)
                    .build());
            } else {
                throw new BusinessException("Cada item precisa ter um produto ou um combo");
            }
        }
        pedido.setItens(itens);

        Pedido saved = pedidoRepository.save(pedido);

        // Mantém a associação bidirecional em memória: `comanda` (lado inverso,
        // mappedBy) não é atualizado automaticamente só por setar `pedido.comanda`.
        // Sem isso, dentro de uma mesma transação que reusa esta MESMA instância
        // gerenciada de Comanda em chamadas subsequentes (ex: PdvService, que
        // abre/fecha/paga a comanda tudo na mesma @Transactional), comanda.getPedidos()
        // ficaria "congelado" vazio pro resto da transação — Hibernate não re-popula
        // uma coleção de uma entidade já gerenciada só porque uma query com
        // @EntityGraph rodou de novo.
        comanda.getPedidos().add(saved);

        verificarEBaixarEstoque(restauranteId, saved, authHeader);

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

        auditoriaService.registrar(restauranteId, "PEDIDO_CRIADO",
            "Pedido #" + saved.getId() + " da mesa " + comanda.getMesaNumero() + " enviado" + (request.isCozinha() ? " para a cozinha" : ""),
            comanda.getGarconId(), comanda.getGarconNome());

        return toResponse(saved);
    }

    // Busca a composição do combo no catalog-service e expande em N ItemPedido
    // reais (produtoId real, preço unitário = valor rateado do combo / qtd da
    // linha) — estoque e cozinha continuam enxergando produtos normais.
    private List<ItemPedido> expandirCombo(Pedido pedido, PedidoRequest.ItemRequest item, Long restauranteId, String authHeader) {
        ComboResponseDto combo;
        try {
            combo = catalogClient.buscarCombo(authHeader, restauranteId, item.getComboId());
        } catch (FeignException.NotFound e) {
            throw new BusinessException("Combo não encontrado");
        } catch (FeignException e) {
            throw new BusinessException(CatalogClientErrorUtil.extrairMensagem(e, "Não foi possível carregar o combo. Tente novamente."));
        }
        if (!combo.isAtivo()) {
            throw new BusinessException("Combo '" + combo.getNome() + "' está inativo");
        }
        int comboQuantidade = item.getComboQuantidade() != null ? item.getComboQuantidade() : 1;
        if (comboQuantidade < 1) {
            throw new BusinessException("Quantidade de combo inválida");
        }

        List<ItemPedido> expandido = new ArrayList<>();
        for (ComboItemDto comboItem : combo.getItens()) {
            BigDecimal precoUnitario = comboItem.getValorAlocado()
                .divide(BigDecimal.valueOf(comboItem.getQuantidade()), 2, RoundingMode.HALF_UP);
            expandido.add(ItemPedido.builder()
                .pedido(pedido)
                .produtoId(comboItem.getProdutoId())
                .produtoNome(comboItem.getProdutoNome())
                .quantidade(comboItem.getQuantidade() * comboQuantidade)
                .observacao(item.getObservacao())
                .precoUnitario(precoUnitario)
                .comboId(combo.getId())
                .comboNome(combo.getNome())
                .build());
        }
        return expandido;
    }

    // Checagem+baixa atômica de estoque no catalog-service — diferente das
    // outras chamadas Feign deste serviço (TableClient etc.), NÃO é engolida
    // em try/catch: estoque insuficiente ou catalog-service inacessível
    // bloqueia a venda (regra de negócio dura, "não vender o que não tem").
    private void verificarEBaixarEstoque(Long restauranteId, Pedido pedido, String authHeader) {
        List<ItemQuantidadeDto> itens = pedido.getItens().stream()
            .map(i -> new ItemQuantidadeDto(i.getProdutoId(), i.getQuantidade()))
            .toList();
        EstoqueBaixaRequestDto request = new EstoqueBaixaRequestDto(itens, "pedido:" + pedido.getId());
        try {
            catalogClient.verificarEBaixarEstoque(authHeader, restauranteId, request);
        } catch (FeignException e) {
            throw new BusinessException(CatalogClientErrorUtil.extrairMensagem(e, "Não foi possível verificar o estoque. Tente novamente."));
        }
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
            // Pedido de delivery não tem comanda — quem é notificado é o
            // entregador (via propagarProntoParaEntrega acima), não o garçom.
            if (saved.getComanda() != null) {
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
            // Pedido de delivery não tem comanda pra notificar (o "entregue"
            // dele é tratado pelo fluxo de Entrega, não por essa notificação).
            if (saved.getComanda() != null) {
                notificationClient.pedidoEntregue(NotificacaoMessage.builder()
                    .tipo("PEDIDO_ENTREGUE")
                    .pedidoId(saved.getId())
                    .comandaId(saved.getComanda().getId())
                    .restauranteId(restauranteId)
                    .mesaNumero(saved.getComanda().getMesaNumero())
                    .build());
            }
        } catch (Exception e) {
            log.warn("Falha ao notificar entrega: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public PedidoResponse cancelar(Long restauranteId, Long id, String authHeader) {
        Pedido pedido = findPedido(restauranteId, id);
        if (pedido.getStatus() != StatusPedido.ENVIADO && pedido.getStatus() != StatusPedido.PREPARANDO) {
            throw new BusinessException("Pedido não pode mais ser cancelado");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        Pedido saved = pedidoRepository.save(pedido);

        // Compensação best-effort: libera o estoque baixado quando o pedido foi
        // enviado. Ao contrário da baixa (verificarEBaixarEstoque), aqui uma
        // falha só implica em contagem de estoque a corrigir manualmente
        // depois, não em overselling — por isso é log-and-swallow.
        try {
            List<ItemQuantidadeDto> itens = saved.getItens().stream()
                .map(i -> new ItemQuantidadeDto(i.getProdutoId(), i.getQuantidade()))
                .toList();
            catalogClient.liberarEstoque(authHeader, restauranteId,
                new EstoqueBaixaRequestDto(itens, "pedido:" + saved.getId()));
        } catch (Exception e) {
            log.warn("Falha ao liberar estoque do pedido {} cancelado: {}", saved.getId(), e.getMessage());
        }

        return toResponse(saved);
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
                .comboId(i.getComboId()).comboNome(i.getComboNome())
                .build())
            .toList();
        // p.getComanda() é null pra pedido de delivery — comanda é exclusiva
        // do fluxo de garçom/mesa (ver DeliveryOrchestrationService).
        Comanda comanda = p.getComanda();
        return PedidoResponse.builder()
            .id(p.getId()).comandaId(comanda != null ? comanda.getId() : null)
            .restauranteId(p.getRestauranteId())
            .entregaId(p.getEntregaId())
            .mesaNumero(comanda != null ? comanda.getMesaNumero() : null)
            .garconId(comanda != null ? comanda.getGarconId() : null)
            .garconNome(comanda != null ? comanda.getGarconNome() : null)
            .status(p.getStatus().name()).observacao(p.getObservacao())
            .itens(itens).criadoEm(p.getCreatedAt()).prontoEm(p.getReadyAt())
            .build();
    }
}
