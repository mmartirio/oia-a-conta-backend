package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.response.ComissaoResponse;
import com.oiaaconta.order.dto.response.ResumoFinanceiroResponse;
import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusComanda;
import com.oiaaconta.order.repository.ComandaRepository;
import com.oiaaconta.order.repository.EntregaRepository;
import com.oiaaconta.order.repository.PedidoRepository;
import com.oiaaconta.order.repository.RestauranteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public ResumoFinanceiroResponse getResumo(Long restauranteId, LocalDateTime inicio, LocalDateTime fim) {
        List<Comanda> comandas = comandaRepository.findByRestauranteIdAndStatusAndClosedAtBetween(
            restauranteId, StatusComanda.FECHADA, inicio, fim);

        List<Entrega> entregas = entregaRepository
            .findByRestauranteIdAndPagamentoConfirmadoCaixaTrueAndEntregueAtBetween(restauranteId, inicio, fim);

        BigDecimal totalComandas = comandas.stream()
            .map(this::calcularTotalComanda)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEntregas = entregas.stream()
            .map(this::calcularTotalEntrega)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

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
            .totalGeral(totalComandas.add(totalEntregas))
            .qtdComandas((long) comandas.size())
            .qtdEntregas((long) entregas.size())
            .breakdownPorMetodo(breakdown)
            .build();
    }

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
        List<Comanda> comandas = comandaRepository.findByRestauranteIdAndStatusAndClosedAtBetween(
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

    private BigDecimal calcularTotalComanda(Comanda c) {
        return c.getPedidos().stream()
            .flatMap(p -> p.getItens().stream())
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTotalEntrega(Entrega e) {
        return e.getItens().stream()
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
