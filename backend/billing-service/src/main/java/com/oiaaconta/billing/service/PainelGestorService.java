package com.oiaaconta.billing.service;

import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.enums.StatusContrato;
import com.oiaaconta.billing.enums.StatusPagamento;
import com.oiaaconta.billing.enums.StatusTicket;
import com.oiaaconta.billing.repository.ContratoRepository;
import com.oiaaconta.billing.repository.PagamentoRepository;
import com.oiaaconta.billing.repository.TicketSuporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

// Dados agregados do painel do gestor (SUPER_ADMIN), calculados aqui de uma
// vez — antes o navegador buscava todos os contratos e depois 1 chamada de
// pagamentos por contrato, o que fica lento conforme a base cresce.
@Service
@RequiredArgsConstructor
public class PainelGestorService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ContratoRepository contratoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final TicketSuporteRepository ticketRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> resumo() {
        LocalDate hoje = LocalDate.now(FUSO);
        List<Contrato> contratos = contratoRepository.findAllByOrderByCreatedAtDesc();
        List<Pagamento> pagamentos = pagamentoRepository.findAllComContrato();

        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusContrato s : StatusContrato.values()) porStatus.put(s.name(), 0L);
        contratos.forEach(c -> porStatus.merge(c.getStatus().name(), 1L, Long::sum));

        // MRR = soma do preço mensal dos contratos que estão pagando (ATIVO).
        BigDecimal mrr = contratos.stream()
            .filter(c -> c.getStatus() == StatusContrato.ATIVO)
            .map(c -> c.getPlano().getPrecoMensal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        YearMonth mesAtual = YearMonth.from(hoje);
        Map<YearMonth, BigDecimal> receitaPorMes = new TreeMap<>();
        for (int i = 11; i >= 0; i--) receitaPorMes.put(mesAtual.minusMonths(i), BigDecimal.ZERO);
        for (Pagamento p : pagamentos) {
            if (p.getStatus() != StatusPagamento.PAGO) continue;
            LocalDate data = p.getDataPagamento() != null ? p.getDataPagamento() : p.getCreatedAt().toLocalDate();
            receitaPorMes.computeIfPresent(YearMonth.from(data), (k, v) -> v.add(p.getValor()));
        }
        List<Map<String, Object>> serieReceita = new ArrayList<>();
        receitaPorMes.forEach((mes, valor) -> serieReceita.add(Map.of("mes", mes.toString(), "receita", valor)));

        // Atenção: o que exige ação do gestor.
        List<Map<String, Object>> vencendo = contratos.stream()
            .filter(c -> c.getStatus() == StatusContrato.ATIVO || c.getStatus() == StatusContrato.TRIAL)
            .filter(c -> c.getDataVencimento() != null
                && !c.getDataVencimento().isBefore(hoje) && !c.getDataVencimento().isAfter(hoje.plusDays(7)))
            .filter(c -> c.getMpPreapprovalId() == null) // com cartão o MP cobra sozinho
            .map(this::contratoResumo)
            .toList();
        List<Map<String, Object>> atrasados = contratos.stream()
            .filter(c -> c.getStatus() != StatusContrato.CANCELADO)
            .filter(c -> c.getStatus() == StatusContrato.INADIMPLENTE || c.getStatus() == StatusContrato.BLOQUEADO
                || (c.getDataVencimento() != null && c.getDataVencimento().isBefore(hoje)))
            .map(this::contratoResumo)
            .toList();

        long pagamentosFalhos30d = pagamentos.stream()
            .filter(p -> p.getStatus() == StatusPagamento.FALHOU)
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().isAfter(hoje.minusDays(30)))
            .count();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalEmpresas", contratos.size());
        r.put("porStatus", porStatus);
        r.put("mrr", mrr);
        r.put("receitaMesAtual", receitaPorMes.get(mesAtual));
        r.put("receitaMesAnterior", receitaPorMes.get(mesAtual.minusMonths(1)));
        r.put("serieReceita", serieReceita);
        r.put("assinaturasCartao", contratos.stream().filter(c -> c.getMpPreapprovalId() != null).count());
        r.put("pagamentosFalhos30d", pagamentosFalhos30d);
        r.put("ticketsAbertos", ticketRepository.countByStatus(StatusTicket.ABERTO)
            + ticketRepository.countByStatus(StatusTicket.EM_ANDAMENTO));
        r.put("vencendo7dias", vencendo);
        r.put("atrasados", atrasados);
        return r;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pagamentos() {
        return pagamentoRepository.findAllComContrato().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("valor", p.getValor());
            m.put("status", p.getStatus().name());
            m.put("metodo", p.getMetodo());
            m.put("observacao", p.getObservacao());
            m.put("dataPagamento", p.getDataPagamento());
            m.put("createdAt", p.getCreatedAt());
            m.put("contratoId", p.getContrato().getId());
            m.put("restauranteId", p.getContrato().getRestauranteId());
            m.put("planoNome", p.getContrato().getPlano().getNome());
            return m;
        }).toList();
    }

    private Map<String, Object> contratoResumo(Contrato c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contratoId", c.getId());
        m.put("restauranteId", c.getRestauranteId());
        m.put("planoNome", c.getPlano().getNome());
        m.put("valor", c.getPlano().getPrecoMensal());
        m.put("status", c.getStatus().name());
        m.put("dataVencimento", c.getDataVencimento());
        m.put("cartao", c.getMpPreapprovalId() != null);
        return m;
    }
}
