package com.oiaaconta.order.service;

import com.oiaaconta.order.client.CatalogClient;
import com.oiaaconta.order.client.CatalogClientErrorUtil;
import com.oiaaconta.order.client.TableClient;
import com.oiaaconta.order.dto.catalog.CupomValidacaoDto;
import com.oiaaconta.order.dto.catalog.PromocaoAplicavelDto;
import com.oiaaconta.order.dto.request.AplicarDescontoRequest;
import com.oiaaconta.order.dto.request.ConfirmarPagamentoRequest;
import com.oiaaconta.order.dto.response.ComandaResponse;
import com.oiaaconta.order.dto.response.ItemPedidoResponse;
import com.oiaaconta.order.dto.response.PedidoResponse;
import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusComanda;
import com.oiaaconta.order.enums.TipoDesconto;
import com.oiaaconta.order.enums.TipoDescontoOrigem;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.exception.ResourceNotFoundException;
import com.oiaaconta.order.repository.ComandaRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComandaService {

    private final ComandaRepository comandaRepository;
    private final TableClient tableClient;
    private final CatalogClient catalogClient;
    private final AuditoriaService auditoriaService;

    @Transactional
    @SuppressWarnings("null")
    public ComandaResponse abrirComanda(Long restauranteId, Long mesaId, Integer mesaNumero,
                                         Long garconId, String garconNome, String authHeader) {
        comandaRepository.findByMesaIdAndStatusAndRestauranteId(mesaId, StatusComanda.ABERTA, restauranteId)
            .ifPresent(c -> { throw new BusinessException("Mesa já possui comanda aberta"); });

        Comanda comanda = comandaRepository.save(Comanda.builder()
            .restauranteId(restauranteId)
            .mesaId(mesaId)
            .mesaNumero(mesaNumero)
            .garconId(garconId)
            .garconNome(garconNome)
            .status(StatusComanda.ABERTA)
            .build());

        try {
            tableClient.atualizarStatus(authHeader, restauranteId, mesaId, Map.of("status", "OCUPADA"));
        } catch (Exception e) {
            log.warn("Falha ao atualizar status da mesa via Feign: {}", e.getMessage());
        }

        return toResponse(comanda);
    }

    public ComandaResponse buscarComandaAtiva(Long restauranteId, Long mesaId) {
        return toResponse(
            comandaRepository.findWithPedidosEItensByMesaIdAndStatusAndRestauranteId(mesaId, StatusComanda.ABERTA, restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma comanda aberta para esta mesa")));
    }

    public ComandaResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(
            comandaRepository.findWithPedidosEItensByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada")));
    }

    public List<ComandaResponse> listarAbertas(Long restauranteId) {
        return comandaRepository.findWithPedidosEItensByRestauranteIdAndStatus(restauranteId, StatusComanda.ABERTA)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ComandaResponse fecharComanda(Long restauranteId, Long id, String authHeader) {
        Comanda comanda = comandaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        if (comanda.getStatus() != StatusComanda.ABERTA) {
            throw new BusinessException("Comanda não está aberta");
        }
        comanda.setStatus(StatusComanda.AGUARDANDO_PAGAMENTO);
        comanda.setAguardandoPagamentoEm(LocalDateTime.now());
        return toResponse(comandaRepository.save(comanda));
    }

    @Transactional
    public ComandaResponse confirmarPagamento(Long restauranteId, Long id,
                                               ConfirmarPagamentoRequest request, String authHeader) {
        Comanda comanda = comandaRepository.findWithPedidosEItensByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        if (comanda.getStatus() != StatusComanda.AGUARDANDO_PAGAMENTO) {
            throw new BusinessException("Comanda não está aguardando pagamento");
        }

        comanda.setStatus(StatusComanda.FECHADA);
        comanda.setMetodoPagamento(request.getMetodoPagamento());
        comanda.setParcelas(request.getParcelas());
        comanda.setClosedAt(LocalDateTime.now());
        // Snapshot persistido no fechamento — desconto não é derivável depois
        // (vem de uma regra externa) e valorTotal evita recalcular a mesma
        // soma em outros lugares (ex: gasto histórico pra promoções).
        BigDecimal subtotal = calcularSubtotal(comanda);
        comanda.setValorTotal(subtotal.subtract(comanda.getDesconto()));
        Comanda saved = comandaRepository.save(comanda);

        try {
            tableClient.atualizarStatus(authHeader, restauranteId, comanda.getMesaId(),
                Map.of("status", "DISPONIVEL"));
        } catch (Exception e) {
            log.warn("Falha ao liberar mesa via Feign: {}", e.getMessage());
        }

        auditoriaService.registrar(restauranteId, "PAGAMENTO_CONFIRMADO",
            "Pagamento da comanda #" + saved.getId() + " confirmado via " + request.getMetodoPagamento()
                + " — valor " + saved.getValorTotal(),
            saved.getGarconId(), saved.getGarconNome());

        return toResponse(saved);
    }

    public List<ComandaResponse> listarAguardandoPagamento(Long restauranteId) {
        return comandaRepository.findWithPedidosEItensByRestauranteIdAndStatus(restauranteId, StatusComanda.AGUARDANDO_PAGAMENTO)
            .stream()
            .map(this::toResponse)
            .sorted(java.util.Comparator.comparing(
                c -> c.getAguardandoPagamentoEm() != null ? c.getAguardandoPagamentoEm() : c.getCriadoEm()))
            .toList();
    }

    // Vincula um cliente identificado à comanda (opcional) — habilita cupom
    // individual/de grupo e promoções automáticas, e alimenta o gasto
    // histórico usado nos requisitos de promoção.
    //
    // Usa a variante SEM fetch-join de pedidos.itens de propósito: se essa
    // chamada acontecer antes do primeiro pedido existir (caso do PDV, que
    // define o cliente logo após abrir a comanda), um fetch-join aqui
    // inicializaria a coleção "pedidos" como vazia — e, como o Hibernate não
    // reconsulta uma coleção já inicializada na mesma sessão, isso deixaria o
    // total zerado mais adiante mesmo depois do pedido ser criado (o mesmo
    // bug de staleness que o fetch-join em confirmarPagamento existe pra
    // evitar). Sem tocar em "pedidos" aqui, o primeiro fetch-join real só
    // acontece depois que o pedido já foi inserido.
    @Transactional
    public ComandaResponse definirCliente(Long restauranteId, Long id, Long clienteId) {
        Comanda comanda = comandaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        if (comanda.getStatus() == StatusComanda.FECHADA) {
            throw new BusinessException("Comanda já está fechada");
        }
        comanda.setClienteId(clienteId);
        return toResponse(comandaRepository.save(comanda));
    }

    // Valida o cupom no catalog-service (elegibilidade de alvo + validade) e
    // aplica o desconto resultante — falha alto (BusinessException) se o
    // cupom não for válido, já que foi uma ação explícita do operador.
    @Transactional
    public ComandaResponse aplicarCupom(Long restauranteId, Long id, String codigo, String authHeader) {
        Comanda comanda = comandaRepository.findWithPedidosEItensByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        validarAbertaOuAguardando(comanda);
        BigDecimal subtotal = calcularSubtotal(comanda);

        CupomValidacaoDto resultado;
        try {
            resultado = catalogClient.validarCupom(authHeader, restauranteId, codigo, comanda.getClienteId());
        } catch (FeignException e) {
            throw new BusinessException(CatalogClientErrorUtil.extrairMensagem(e, "Não foi possível validar o cupom. Tente novamente."));
        }
        if (!resultado.isValido()) {
            throw new BusinessException(resultado.getMotivoInvalido() != null ? resultado.getMotivoInvalido() : "Cupom inválido");
        }

        BigDecimal valorDesconto = calcularValorDesconto(resultado.getTipoDesconto(), resultado.getValorDesconto(), subtotal);
        return aplicarDescontoInterno(comanda, TipoDescontoOrigem.CUPOM, resultado.getCupomId(),
            "Cupom " + resultado.getCodigo(), valorDesconto, subtotal);
    }

    // Revalida a promoção contra a lista de promoções aplicáveis do
    // catalog-service (nunca confia em tipoDesconto/valorDesconto vindo do
    // cliente) e aplica o desconto resultante.
    @Transactional
    public ComandaResponse aplicarPromocao(Long restauranteId, Long id, Long promocaoId, String authHeader) {
        Comanda comanda = comandaRepository.findWithPedidosEItensByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        validarAbertaOuAguardando(comanda);
        BigDecimal subtotal = calcularSubtotal(comanda);
        BigDecimal gastoHistorico = comanda.getClienteId() != null
            ? comandaRepository.gastoHistoricoCliente(restauranteId, comanda.getClienteId())
            : BigDecimal.ZERO;

        List<PromocaoAplicavelDto> aplicaveis;
        try {
            aplicaveis = catalogClient.promocoesAplicaveis(authHeader, restauranteId, comanda.getClienteId(), gastoHistorico);
        } catch (FeignException e) {
            throw new BusinessException(CatalogClientErrorUtil.extrairMensagem(e, "Não foi possível validar a promoção. Tente novamente."));
        }
        PromocaoAplicavelDto escolhida = aplicaveis.stream()
            .filter(p -> p.getPromocaoId().equals(promocaoId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Promoção não é válida para esta comanda"));

        BigDecimal valorDesconto = calcularValorDesconto(escolhida.getTipoDesconto(), escolhida.getValorDesconto(), subtotal);
        return aplicarDescontoInterno(comanda, TipoDescontoOrigem.PROMOCAO, escolhida.getPromocaoId(),
            escolhida.getNome(), valorDesconto, subtotal);
    }

    @Transactional
    public ComandaResponse removerDesconto(Long restauranteId, Long id) {
        Comanda comanda = comandaRepository.findWithPedidosEItensByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        comanda.setDescontoTipo(null);
        comanda.setDescontoOrigemId(null);
        comanda.setDescontoOrigemDescricao(null);
        comanda.setDesconto(BigDecimal.ZERO);
        return toResponse(comandaRepository.save(comanda));
    }

    // @Transactional aqui (não só nos métodos internos aplicarCupom/aplicarPromocao)
    // porque eles são chamados por auto-invocação (this.aplicarCupom(...)), que o
    // proxy do Spring não intercepta — sem isso, a chamada rodaria sem transação.
    @Transactional
    public ComandaResponse aplicarDesconto(Long restauranteId, Long id, AplicarDescontoRequest request, String authHeader) {
        if (request.getTipo() == TipoDescontoOrigem.CUPOM) {
            if (request.getCodigo() == null || request.getCodigo().isBlank()) {
                throw new BusinessException("Código do cupom obrigatório");
            }
            return aplicarCupom(restauranteId, id, request.getCodigo(), authHeader);
        }
        if (request.getPromocaoId() == null) {
            throw new BusinessException("Promoção obrigatória");
        }
        return aplicarPromocao(restauranteId, id, request.getPromocaoId(), authHeader);
    }

    private ComandaResponse aplicarDescontoInterno(Comanda comanda, TipoDescontoOrigem tipo, Long origemId,
                                                     String descricao, BigDecimal valorDesconto, BigDecimal subtotal) {
        comanda.setDescontoTipo(tipo);
        comanda.setDescontoOrigemId(origemId);
        comanda.setDescontoOrigemDescricao(descricao);
        // Nunca deixa o desconto ultrapassar o subtotal (total não fica negativo).
        comanda.setDesconto(valorDesconto.min(subtotal));
        return toResponse(comandaRepository.save(comanda));
    }

    private BigDecimal calcularValorDesconto(TipoDesconto tipo, BigDecimal valor, BigDecimal subtotal) {
        if (tipo == TipoDesconto.PERCENTUAL) {
            return subtotal.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return valor;
    }

    private void validarAbertaOuAguardando(Comanda comanda) {
        if (comanda.getStatus() == StatusComanda.FECHADA) {
            throw new BusinessException("Comanda já está fechada");
        }
    }

    private BigDecimal calcularSubtotal(Comanda c) {
        return c.getPedidos().stream()
            .flatMap(p -> p.getItens().stream())
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @SuppressWarnings("null")
    ComandaResponse toResponse(Comanda c) {
        List<PedidoResponse> pedidosResp = c.getPedidos().stream()
            .map(this::pedidoToResponse).toList();

        BigDecimal subtotal = calcularSubtotal(c);
        BigDecimal desconto = c.getDesconto() != null ? c.getDesconto() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(desconto);

        return ComandaResponse.builder()
            .id(c.getId()).restauranteId(c.getRestauranteId())
            .mesaId(c.getMesaId()).mesaNumero(c.getMesaNumero())
            .garconId(c.getGarconId()).garconNome(c.getGarconNome())
            .status(c.getStatus().name())
            .metodoPagamento(c.getMetodoPagamento() != null ? c.getMetodoPagamento().name() : null)
            .parcelas(c.getParcelas())
            .subtotal(subtotal).desconto(desconto).total(total)
            .clienteId(c.getClienteId())
            .descontoTipo(c.getDescontoTipo() != null ? c.getDescontoTipo().name() : null)
            .descontoOrigemDescricao(c.getDescontoOrigemDescricao())
            .pedidos(pedidosResp)
            .criadoEm(c.getCreatedAt()).fechadoEm(c.getClosedAt())
            .aguardandoPagamentoEm(c.getAguardandoPagamentoEm())
            .build();
    }

    private PedidoResponse pedidoToResponse(Pedido p) {
        List<ItemPedidoResponse> itens = p.getItens().stream()
            .map(i -> ItemPedidoResponse.builder()
                .id(i.getId()).produtoId(i.getProdutoId())
                .produtoNome(i.getProdutoNome()).quantidade(i.getQuantidade())
                .observacao(i.getObservacao()).precoUnitario(i.getPrecoUnitario())
                .subtotal(i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .comboId(i.getComboId()).comboNome(i.getComboNome())
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
