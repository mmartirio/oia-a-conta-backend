package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.response.ComissaoResponse;
import com.oiaaconta.order.dto.response.ComparativoResponse;
import com.oiaaconta.order.dto.response.EvolucaoDiariaResponse;
import com.oiaaconta.order.dto.response.ResumoFinanceiroResponse;
import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.entity.Despesa;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusComanda;
import com.oiaaconta.order.enums.StatusPedido;
import com.oiaaconta.order.repository.ComandaRepository;
import com.oiaaconta.order.repository.DespesaRepository;
import com.oiaaconta.order.repository.EntregaRepository;
import com.oiaaconta.order.repository.PedidoRepository;
import com.oiaaconta.order.repository.RestauranteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final ComandaRepository comandaRepository;
    private final EntregaRepository entregaRepository;
    private final PedidoRepository pedidoRepository;
    private final RestauranteConfigRepository configRepository;
    private final DespesaRepository despesaRepository;

    @SuppressWarnings("null")
    public ResumoFinanceiroResponse getResumo(Long restauranteId, LocalDateTime inicio, LocalDateTime fim) {
        List<Comanda> comandas = comandaRepository.findWithPedidosEItensByRestauranteIdAndStatusAndClosedAtBetween(
            restauranteId, StatusComanda.FECHADA, inicio, fim);

        List<Entrega> entregas = entregaRepository
            .findByRestauranteIdAndPagamentoConfirmadoCaixaTrueAndEntregueAtBetween(restauranteId, inicio, fim);

        BigDecimal totalComandas = comandas.stream()
            .map(this::calcularTotalComanda)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEntregas = entregas.stream()
            .map(this::calcularTotalEntrega)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGeral = totalComandas.add(totalEntregas);

        BigDecimal totalDespesas = despesaRepository.somarPorPeriodo(
            restauranteId, inicio.toLocalDate(), fim.toLocalDate());
        if (totalDespesas == null) totalDespesas = BigDecimal.ZERO;

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        for (Comanda c : comandas) {
            if (c.getMetodoPagamento() != null) {
                breakdown.merge(c.getMetodoPagamento().name(), calcularTotalComanda(c), BigDecimal::add);
            }
        }
        for (Entrega e : entregas) {
            breakdown.merge(e.getMetodoPagamento().name(), calcularTotalEntrega(e), BigDecimal::add);
        }

        return ResumoFinanceiroResponse.builder()
            .totalComandas(totalComandas)
            .totalEntregas(totalEntregas)
            .totalGeral(totalGeral)
            .totalDespesas(totalDespesas)
            .lucroLiquido(totalGeral.subtract(totalDespesas))
            .qtdComandas((long) comandas.size())
            .qtdEntregas((long) entregas.size())
            .breakdownPorMetodo(breakdown)
            .build();
    }

    @SuppressWarnings("null")
    public List<EvolucaoDiariaResponse> getEvolucao(Long restauranteId, LocalDateTime inicio, LocalDateTime fim) {
        List<Comanda> comandas = comandaRepository.findWithPedidosEItensByRestauranteIdAndStatusAndClosedAtBetween(
            restauranteId, StatusComanda.FECHADA, inicio, fim);
        List<Entrega> entregas = entregaRepository
            .findByRestauranteIdAndPagamentoConfirmadoCaixaTrueAndEntregueAtBetween(restauranteId, inicio, fim);
        List<Despesa> despesas = despesaRepository.findByRestauranteIdAndDataBetweenOrderByDataDesc(
            restauranteId, inicio.toLocalDate(), fim.toLocalDate());

        Map<LocalDate, BigDecimal> comandasPorDia = comandas.stream()
            .collect(Collectors.groupingBy(
                c -> c.getClosedAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, this::calcularTotalComanda, BigDecimal::add)));

        Map<LocalDate, BigDecimal> entregasPorDia = entregas.stream()
            .collect(Collectors.groupingBy(
                e -> e.getEntregueAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, this::calcularTotalEntrega, BigDecimal::add)));

        Map<LocalDate, BigDecimal> despesasPorDia = despesas.stream()
            .collect(Collectors.groupingBy(Despesa::getData,
                Collectors.reducing(BigDecimal.ZERO, Despesa::getValor, BigDecimal::add)));

        TreeSet<LocalDate> dias = new TreeSet<>();
        dias.addAll(comandasPorDia.keySet());
        dias.addAll(entregasPorDia.keySet());
        dias.addAll(despesasPorDia.keySet());

        return dias.stream()
            .map(dia -> EvolucaoDiariaResponse.builder()
                .data(dia)
                .totalComandas(comandasPorDia.getOrDefault(dia, BigDecimal.ZERO))
                .totalEntregas(entregasPorDia.getOrDefault(dia, BigDecimal.ZERO))
                .totalDespesas(despesasPorDia.getOrDefault(dia, BigDecimal.ZERO))
                .build())
            .toList();
    }

    public ComparativoResponse getComparativo(Long restauranteId, LocalDateTime inicio, LocalDateTime fim) {
        Duration duracao = Duration.between(inicio, fim);
        LocalDateTime inicioAnterior = inicio.minus(duracao);
        LocalDateTime fimAnterior = inicio;

        ResumoFinanceiroResponse atual = getResumo(restauranteId, inicio, fim);
        ResumoFinanceiroResponse anterior = getResumo(restauranteId, inicioAnterior, fimAnterior);

        BigDecimal variacaoPercentual;
        if (anterior.getTotalGeral() == null || anterior.getTotalGeral().compareTo(BigDecimal.ZERO) == 0) {
            variacaoPercentual = null;
        } else {
            variacaoPercentual = atual.getTotalGeral().subtract(anterior.getTotalGeral())
                .divide(anterior.getTotalGeral(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        return ComparativoResponse.builder()
            .atual(atual)
            .anterior(anterior)
            .variacaoPercentual(variacaoPercentual)
            .build();
    }

    @SuppressWarnings("null")
    // Base de cálculo vem só das vendas (comandas/entregas) — a diferença de
    // caixa apurada em CaixaService.fechar() nunca deve entrar aqui: quebra de
    // caixa é risco do negócio, não desconto automático da comissão/salário
    // de quem fechou o caixa.
    public List<ComissaoResponse> getComissoes(Long restauranteId, LocalDateTime inicio, LocalDateTime fim) {
        var config = configRepository.findByRestauranteId(restauranteId);
        BigDecimal pctGarcon = config.map(c -> c.getComissaoGarcon() != null ? c.getComissaoGarcon() : BigDecimal.ZERO)
            .orElse(BigDecimal.ZERO);
        BigDecimal pctEntregador = config.map(c -> c.getComissaoEntregador() != null ? c.getComissaoEntregador() : BigDecimal.ZERO)
            .orElse(BigDecimal.ZERO);
        BigDecimal pctCozinheiro = config.map(c -> c.getComissaoCozinheiro() != null ? c.getComissaoCozinheiro() : BigDecimal.ZERO)
            .orElse(BigDecimal.ZERO);

        List<ComissaoResponse> result = new ArrayList<>();

        // Garçons: soma das comandas fechadas por garçom
        List<Comanda> comandas = comandaRepository.findWithPedidosEItensByRestauranteIdAndStatusAndClosedAtBetween(
            restauranteId, StatusComanda.FECHADA, inicio, fim);

        comandas.stream()
            .collect(Collectors.groupingBy(c -> c.getGarconId() + "||" + c.getGarconNome()))
            .forEach((key, lista) -> {
                String[] parts = key.split("\\|\\|");
                Long id = Long.parseLong(parts[0]);
                String nome = parts[1];
                BigDecimal base = lista.stream().map(this::calcularTotalComanda).reduce(BigDecimal.ZERO, BigDecimal::add);
                result.add(ComissaoResponse.builder()
                    .funcionarioId(id).nome(nome).role("GARCON")
                    .totalBase(base).percentual(pctGarcon)
                    .valorComissao(base.multiply(pctGarcon).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                    .build());
            });

        // Entregadores: soma das entregas confirmadas por entregador
        List<Entrega> entregas = entregaRepository
            .findByRestauranteIdAndPagamentoConfirmadoCaixaTrueAndEntregueAtBetween(restauranteId, inicio, fim);

        entregas.stream()
            .filter(e -> e.getEntregadorId() != null)
            .collect(Collectors.groupingBy(e -> e.getEntregadorId() + "||" + e.getEntregadorNome()))
            .forEach((key, lista) -> {
                String[] parts = key.split("\\|\\|");
                Long id = Long.parseLong(parts[0]);
                String nome = parts[1];
                BigDecimal base = lista.stream().map(this::calcularTotalEntrega).reduce(BigDecimal.ZERO, BigDecimal::add);
                result.add(ComissaoResponse.builder()
                    .funcionarioId(id).nome(nome).role("ENTREGADOR")
                    .totalBase(base).percentual(pctEntregador)
                    .valorComissao(base.multiply(pctEntregador).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                    .build());
            });

        // Cozinheiros: soma dos pedidos por cozinheiro
        List<Pedido> pedidos = pedidoRepository
            .findByRestauranteIdAndCozinheiroIdNotNullAndCreatedAtBetween(restauranteId, inicio, fim);

        pedidos.stream()
            .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
            .collect(Collectors.groupingBy(p -> p.getCozinheiroId() + "||" + p.getCozinheiroNome()))
            .forEach((key, lista) -> {
                String[] parts = key.split("\\|\\|");
                Long id = Long.parseLong(parts[0]);
                String nome = parts.length > 1 ? parts[1] : "Cozinheiro";
                BigDecimal base = lista.stream()
                    .flatMap(p -> p.getItens().stream())
                    .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                result.add(ComissaoResponse.builder()
                    .funcionarioId(id).nome(nome).role("COZINHEIRO")
                    .totalBase(base).percentual(pctCozinheiro)
                    .valorComissao(base.multiply(pctCozinheiro).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                    .build());
            });

        return result;
    }

    // Comandas fechadas depois da introdução de cupom/promoção já têm
    // valorTotal persistido (subtotal - desconto) em confirmarPagamento — usa
    // esse snapshot quando existir. Comandas fechadas antes disso (valorTotal
    // NULL) caem no cálculo antigo a partir dos itens (equivalente, já que
    // não tinham desconto).
    @SuppressWarnings("null")
    private BigDecimal calcularTotalComanda(Comanda c) {
        if (c.getValorTotal() != null) {
            return c.getValorTotal();
        }
        BigDecimal subtotal = c.getPedidos().stream()
            .flatMap(p -> p.getItens().stream())
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desconto = c.getDesconto() != null ? c.getDesconto() : BigDecimal.ZERO;
        return subtotal.subtract(desconto);
    }

    @SuppressWarnings("null")
    private BigDecimal calcularTotalEntrega(Entrega e) {
        return e.getItens().stream()
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
