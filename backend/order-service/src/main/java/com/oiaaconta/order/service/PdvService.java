package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.AplicarDescontoRequest;
import com.oiaaconta.order.dto.request.ConfirmarPagamentoRequest;
import com.oiaaconta.order.dto.request.PedidoRequest;
import com.oiaaconta.order.dto.request.VendaBalcaoRequest;
import com.oiaaconta.order.dto.response.ComandaResponse;
import com.oiaaconta.order.enums.MetodoPagamento;
import com.oiaaconta.order.enums.TipoDescontoOrigem;
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

        if (request.getClienteId() != null) {
            comandaService.definirCliente(restauranteId, comanda.getId(), request.getClienteId());
        }

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
                itemRequest.setComboId(item.getComboId());
                itemRequest.setComboQuantidade(item.getComboQuantidade());
                return itemRequest;
            })
            .toList());

        // Baixa/verifica estoque (bloqueia a venda se insuficiente) e expande
        // combos — feito dentro de enviarParaCozinha, único ponto de criação
        // de ItemPedido compartilhado com o fluxo de comanda de mesa.
        pedidoService.enviarParaCozinha(restauranteId, comanda.getId(), pedidoRequest, authHeader);

        if (request.getCupomCodigo() != null && !request.getCupomCodigo().isBlank()) {
            AplicarDescontoRequest descontoRequest = new AplicarDescontoRequest();
            descontoRequest.setTipo(TipoDescontoOrigem.CUPOM);
            descontoRequest.setCodigo(request.getCupomCodigo());
            comandaService.aplicarDesconto(restauranteId, comanda.getId(), descontoRequest, authHeader);
        }

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

        // confirmarPagamento recarrega a comanda com pedidos+itens numa query
        // própria (findWithPedidosEItensByIdAndRestauranteId), então o
        // subtotal/total do response já vem correto — sem precisar
        // reconstruir o total a partir do request aqui.
        return comandaService.confirmarPagamento(restauranteId, comanda.getId(), confirmarPagamentoRequest, authHeader);
    }
}
