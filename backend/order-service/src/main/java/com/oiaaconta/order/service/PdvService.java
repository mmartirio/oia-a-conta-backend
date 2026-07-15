package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.ConfirmarPagamentoRequest;
import com.oiaaconta.order.dto.request.PedidoRequest;
import com.oiaaconta.order.dto.request.VendaBalcaoRequest;
import com.oiaaconta.order.dto.response.ComandaResponse;
import com.oiaaconta.order.enums.MetodoPagamento;
import com.oiaaconta.order.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a venda de balcão do PDV reaproveitando ComandaService/PedidoService,
 * sem duplicar as regras de negócio já existentes para comandas/pedidos de mesa.
 * Usa a convenção de "mesa virtual" mesaId=0/mesaNumero=0 já adotada para
 * delivery em DeliveryOrchestrationService, e abre+fecha a comanda de forma
 * síncrona dentro da mesma chamada (venda de balcão é instantânea).
 */
@Service
@RequiredArgsConstructor
public class PdvService {

    private final ComandaService comandaService;
    private final PedidoService pedidoService;

    @Transactional
    public ComandaResponse criarVenda(Long restauranteId, Long caixaUserId, String caixaNome,
                                       VendaBalcaoRequest request, String authHeader) {
        // mesaId=-1 é a "mesa virtual" própria do PDV balcão, distinta do
        // mesaId=0 já usado pelo Delivery (DeliveryOrchestrationService) —
        // reaproveitar o mesmo 0 colide com o "já possui comanda aberta" e
        // pode disparar NonUniqueResultException quando há >1 delivery ABERTA.
        ComandaResponse comanda = comandaService.abrirComanda(
            restauranteId, -1L, 0, caixaUserId, caixaNome != null ? caixaNome : "PDV Balcão", authHeader);

        PedidoRequest pedidoRequest = new PedidoRequest();
        pedidoRequest.setObservacao(request.getObservacao());
        pedidoRequest.setCozinha(true);
        pedidoRequest.setItens(request.getItens().stream()
            .map(item -> {
                PedidoRequest.ItemRequest itemRequest = new PedidoRequest.ItemRequest();
                itemRequest.setProdutoId(item.getProdutoId());
                itemRequest.setProdutoNome(item.getProdutoNome());
                itemRequest.setQuantidade(item.getQuantidade());
                itemRequest.setPrecoUnitario(item.getPrecoUnitario());
                itemRequest.setObservacao(item.getObservacao());
                return itemRequest;
            })
            .toList());

        pedidoService.enviarParaCozinha(restauranteId, comanda.getId(), pedidoRequest);

        comandaService.fecharComanda(restauranteId, comanda.getId(), authHeader);

        MetodoPagamento metodoPagamento;
        try {
            metodoPagamento = MetodoPagamento.valueOf(request.getMetodoPagamento());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Método de pagamento inválido: " + request.getMetodoPagamento());
        }

        ConfirmarPagamentoRequest confirmarPagamentoRequest = new ConfirmarPagamentoRequest();
        confirmarPagamentoRequest.setMetodoPagamento(metodoPagamento);
        confirmarPagamentoRequest.setParcelas(request.getParcelas());

        ComandaResponse response = comandaService.confirmarPagamento(
            restauranteId, comanda.getId(), confirmarPagamentoRequest, authHeader);

        // O "pedidos"/"total" de response costuma voltar vazio/zerado aqui:
        // dentro desta mesma transação, ComandaService.abrirComanda já
        // inicializou a coleção "pedidos" da comanda (ainda vazia, via seu
        // próprio toResponse) e o Hibernate não a recarrega sozinho depois
        // que o pedido é inserido — a linha no banco fica correta (outras
        // consultas, ex. resumo-dia, já a enxergam certo), só o corpo desta
        // resposta específica que fica desatualizado. Recalcula aqui a
        // partir do request em vez de mexer na entidade JPA (uma tentativa
        // anterior de sincronizar a coleção em memória disparava
        // UnsupportedOperationException do Hibernate ao mesclar a comanda
        // de volta em fecharComanda/confirmarPagamento).
        java.math.BigDecimal total = request.getItens().stream()
            .map(item -> (item.getPrecoUnitario() != null ? item.getPrecoUnitario() : java.math.BigDecimal.ZERO)
                .multiply(java.math.BigDecimal.valueOf(item.getQuantidade())))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        response.setTotal(total);

        return response;
    }
}
