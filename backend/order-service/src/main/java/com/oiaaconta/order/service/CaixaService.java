package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.AbrirCaixaRequest;
import com.oiaaconta.order.dto.request.FecharCaixaRequest;
import com.oiaaconta.order.dto.response.ResumoFinanceiroResponse;
import com.oiaaconta.order.dto.response.SessaoCaixaResponse;
import com.oiaaconta.order.dto.response.VendaResponse;
import com.oiaaconta.order.entity.SessaoCaixa;
import com.oiaaconta.order.enums.StatusSessaoCaixa;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.repository.SessaoCaixaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaixaService {

    private final SessaoCaixaRepository sessaoCaixaRepository;
    private final FinanceiroService financeiroService;

    public SessaoCaixaResponse status(Long restauranteId) {
        return sessaoCaixaRepository
            .findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .map(this::toResponse)
            .orElse(null);
    }

    public List<SessaoCaixaResponse> historico(Long restauranteId) {
        return sessaoCaixaRepository.findByRestauranteIdOrderByAbertoEmDesc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    // Resumo do que entrou desde a abertura da sessão até agora (comandas
    // fechadas + entregas com pagamento confirmado no caixa, somadas por
    // forma de pagamento) — pra conferência na hora de fechar o caixa,
    // comparando com o valor contado manualmente. Reaproveita o mesmo
    // cálculo do relatório financeiro (FinanceiroService.getResumo), só
    // trocando o período pelo intervalo da sessão aberta.
    public ResumoFinanceiroResponse resumo(Long restauranteId) {
        SessaoCaixa sessao = sessaoCaixaRepository
            .findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .orElseThrow(() -> new BusinessException("Não há caixa aberto"));
        return financeiroService.getResumo(restauranteId, sessao.getAbertoEm(), LocalDateTime.now());
    }

    // Vendas individuais (não agregadas) de hoje — complementa o resumo() na
    // Fila de Pagamento, mostrando o que já foi vendido/recebido. Restrito
    // ao dia de hoje mesmo que o caixa tenha sido aberto antes da meia-noite
    // (caixa que fica aberto de um dia pro outro) — pra ver vendas de dias
    // anteriores, ver listarVendasHistorico.
    public List<VendaResponse> listarVendas(Long restauranteId) {
        sessaoCaixaRepository
            .findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .orElseThrow(() -> new BusinessException("Não há caixa aberto"));
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        return financeiroService.listarVendas(restauranteId, inicioDia, LocalDateTime.now());
    }

    // Vendas individuais de um dia específico no passado — janela de
    // histórico de vendas, independe de haver caixa aberto ou não.
    public List<VendaResponse> listarVendasHistorico(Long restauranteId, LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.plusDays(1).atStartOfDay();
        return financeiroService.listarVendas(restauranteId, inicio, fim);
    }

    @Transactional
    public SessaoCaixaResponse abrir(Long restauranteId, Long usuarioId, String usuarioNome, AbrirCaixaRequest request) {
        sessaoCaixaRepository.findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .ifPresent(s -> { throw new BusinessException("Já existe um caixa aberto"); });

        SessaoCaixa sessao = sessaoCaixaRepository.save(SessaoCaixa.builder()
            .restauranteId(restauranteId)
            .abertoPorId(usuarioId)
            .abertoPorNome(usuarioNome)
            .valorAbertura(request.getValorAbertura())
            .status(StatusSessaoCaixa.ABERTO)
            .build());
        return toResponse(sessao);
    }

    @Transactional
    public SessaoCaixaResponse fechar(Long restauranteId, Long usuarioId, String usuarioNome, FecharCaixaRequest request) {
        SessaoCaixa sessao = sessaoCaixaRepository
            .findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .orElseThrow(() -> new BusinessException("Não há caixa aberto"));

        // Só o que passa pela gaveta física conta pro esperado — PIX e cartão
        // são liquidados fora do caixa e não devem entrar nessa conta, senão
        // a diferença fica artificialmente enorme sempre que a maior parte das
        // vendas for em cartão/PIX.
        var resumoSessao = financeiroService.getResumo(restauranteId, sessao.getAbertoEm(), java.time.LocalDateTime.now());
        java.math.BigDecimal vendasDinheiro = resumoSessao.getBreakdownPorMetodo()
            .getOrDefault("DINHEIRO", java.math.BigDecimal.ZERO);
        java.math.BigDecimal valorEsperado = sessao.getValorAbertura().add(vendasDinheiro);
        java.math.BigDecimal diferenca = request.getValorFechamento().subtract(valorEsperado);

        sessao.setStatus(StatusSessaoCaixa.FECHADO);
        sessao.setValorFechamento(request.getValorFechamento());
        sessao.setValorEsperadoDinheiro(valorEsperado);
        sessao.setDiferencaCaixa(diferenca);
        sessao.setFechadoPorId(usuarioId);
        sessao.setFechadoPorNome(usuarioNome);
        sessao.setFechadoEm(java.time.LocalDateTime.now());
        return toResponse(sessaoCaixaRepository.save(sessao));
    }

    private SessaoCaixaResponse toResponse(SessaoCaixa s) {
        return SessaoCaixaResponse.builder()
            .id(s.getId())
            .abertoPorNome(s.getAbertoPorNome())
            .valorAbertura(s.getValorAbertura())
            .abertoEm(s.getAbertoEm())
            .fechadoPorNome(s.getFechadoPorNome())
            .valorFechamento(s.getValorFechamento())
            .valorEsperadoDinheiro(s.getValorEsperadoDinheiro())
            .diferencaCaixa(s.getDiferencaCaixa())
            .fechadoEm(s.getFechadoEm())
            .status(s.getStatus())
            .build();
    }
}
