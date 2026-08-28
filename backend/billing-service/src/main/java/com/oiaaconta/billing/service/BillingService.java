package com.oiaaconta.billing.service;

import com.mercadopago.resources.payment.Payment;
import com.oiaaconta.billing.client.AuthInternalClient;
import com.oiaaconta.billing.client.MercadoPagoClient;
import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.entity.LinkSocial;
import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.entity.Plano;
import com.oiaaconta.billing.enums.StatusContrato;
import com.oiaaconta.billing.enums.StatusPagamento;
import com.oiaaconta.billing.repository.ContratoRepository;
import com.oiaaconta.billing.repository.LinkSocialRepository;
import com.oiaaconta.billing.repository.PagamentoRepository;
import com.oiaaconta.billing.repository.PlanoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final PlanoRepository planoRepository;
    private final ContratoRepository contratoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final LinkSocialRepository linkSocialRepository;
    private final AuthInternalClient authInternalClient;
    private final MercadoPagoClient mercadoPagoClient;

    // ─── Planos ───────────────────────────────────────────────────────────────

    public List<Plano> listarPlanosAtivos() {
        return planoRepository.findByAtivoTrueOrderByPrecoMensalAsc();
    }

    public List<Plano> listarTodosPlanos() {
        return planoRepository.findAll();
    }

    @Transactional
    @SuppressWarnings("null")
    public Plano criarPlano(Plano plano) {
        return planoRepository.save(plano);
    }

    @Transactional
    @SuppressWarnings("null")
    public Plano atualizarPlano(Long id, Plano dados) {
        Plano plano = planoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Plano não encontrado"));
        if (dados == null) {
            throw new IllegalArgumentException("Dados do plano são obrigatórios");
        }
        plano.setNome(dados.getNome());
        plano.setDescricao(dados.getDescricao());
        plano.setPrecoMensal(dados.getPrecoMensal());
        plano.setLimiteUsuarios(dados.getLimiteUsuarios());
        plano.setLimiteMesas(dados.getLimiteMesas());
        plano.setFuncionalidades(dados.getFuncionalidades());
        plano.setAtivo(dados.isAtivo());
        plano.setDestaque(dados.isDestaque());
        return planoRepository.save(plano);
    }

    // ─── Links Sociais ──────────────────────────────────────────────────────────

    public List<LinkSocial> listarLinksSociaisAtivos() {
        return linkSocialRepository.findByAtivoTrueOrderByTipoAsc();
    }

    public List<LinkSocial> listarTodosLinksSociais() {
        return linkSocialRepository.findAll();
    }

    @Transactional
    @SuppressWarnings("null")
    public LinkSocial criarLinkSocial(LinkSocial link) {
        return linkSocialRepository.save(link);
    }

    @Transactional
    @SuppressWarnings("null")
    public LinkSocial atualizarLinkSocial(Long id, LinkSocial dados) {
        LinkSocial link = linkSocialRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Link social não encontrado"));
        if (dados == null) {
            throw new IllegalArgumentException("Dados do link são obrigatórios");
        }
        link.setTipo(dados.getTipo());
        link.setUrl(dados.getUrl());
        link.setAtivo(dados.isAtivo());
        return linkSocialRepository.save(link);
    }

    @Transactional
    public void deletarLinkSocial(Long id) {
        if (!linkSocialRepository.existsById(id)) {
            throw new NoSuchElementException("Link social não encontrado");
        }
        linkSocialRepository.deleteById(id);
    }

    // ─── Contratos ────────────────────────────────────────────────────────────

    public Page<Contrato> listarContratos(Pageable pageable) {
        return contratoRepository.findAll(pageable);
    }

    public Contrato buscarContratoDoRestaurante(Long restauranteId) {
        if (restauranteId == null) {
            throw new IllegalArgumentException("restauranteId é obrigatório");
        }
        return contratoRepository.findByRestauranteId(restauranteId)
            .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado"));
    }

    // Chamado por auth-service (login/me) e table-service (criar mesa) pra
    // aplicar os recursos do plano contratado — funcionalidades liberadas e
    // limites de usuários/mesas. Sem contrato encontrado, devolve tudo null
    // (sem restrição) em vez de erro — não deve bloquear login/uso por causa
    // de uma conta sem contrato associado.
    public com.oiaaconta.billing.dto.response.PlanoLimitesResponse buscarLimitesPlano(Long restauranteId) {
        return contratoRepository.findByRestauranteId(restauranteId)
            .map(c -> com.oiaaconta.billing.dto.response.PlanoLimitesResponse.builder()
                .funcionalidades(c.getPlano().getFuncionalidades())
                .limiteUsuarios(c.getPlano().getLimiteUsuarios())
                .limiteMesas(c.getPlano().getLimiteMesas())
                .build())
            .orElse(com.oiaaconta.billing.dto.response.PlanoLimitesResponse.builder().build());
    }

    @Transactional
    @SuppressWarnings("null")
    public Contrato criarContrato(Long restauranteId, Long planoId) {
        if (restauranteId == null || planoId == null) {
            throw new IllegalArgumentException("restauranteId e planoId são obrigatórios");
        }
        if (contratoRepository.findByRestauranteId(restauranteId).isPresent()) {
            throw new IllegalStateException("Restaurante já possui contrato");
        }
        Plano plano = planoRepository.findById(planoId)
            .orElseThrow(() -> new NoSuchElementException("Plano não encontrado"));
        LocalDate hoje = LocalDate.now();
        return contratoRepository.save(Contrato.builder()
            .restauranteId(restauranteId)
            .plano(plano)
            .status(StatusContrato.TRIAL)
            .dataInicio(hoje)
            .dataVencimento(hoje.plusDays(30))
            .dataProximoVencimento(hoje.plusDays(30))
            .build());
    }

    @Transactional
    @SuppressWarnings("null")
    public Contrato atualizarStatusContrato(Long contratoId, StatusContrato novoStatus) {
        Contrato contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado"));
        contrato.setStatus(novoStatus);
        if (novoStatus == StatusContrato.BLOQUEADO) {
            notificarBloqueio(contrato.getRestauranteId());
        } else if (novoStatus == StatusContrato.ATIVO) {
            try { authInternalClient.desbloquear(contrato.getRestauranteId()); } catch (Exception e) {
                log.warn("Falha ao desbloquear restaurante {}: {}", contrato.getRestauranteId(), e.getMessage());
            }
        }
        return contratoRepository.save(contrato);
    }

    // ─── Pagamentos ───────────────────────────────────────────────────────────

    public Page<Pagamento> listarPagamentosDoContrato(Long contratoId, Pageable pageable) {
        return pagamentoRepository.findByContratoIdOrderByCreatedAtDesc(contratoId, pageable);
    }

    @Transactional
    @SuppressWarnings("null")
    public Pagamento registrarPagamentoManual(Long contratoId, BigDecimal valor, String observacao) {
        if (contratoId == null || valor == null) {
            throw new IllegalArgumentException("contratoId e valor são obrigatórios");
        }
        Contrato contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado"));
        Pagamento pag = pagamentoRepository.save(Pagamento.builder()
            .contrato(contrato)
            .valor(valor)
            .status(StatusPagamento.PAGO)
            .dataPagamento(LocalDate.now())
            .metodo("MANUAL")
            .observacao(observacao)
            .build());
        ativarContratoAposPagamento(contrato);
        return pag;
    }

    // ─── Mercado Pago ───────────────────────────────────────────────────────────

    // Gera o link do Checkout Pro pra o restaurante pagar a assinatura online.
    public String criarCobrancaAssinatura(Long restauranteId) {
        Contrato contrato = buscarContratoDoRestaurante(restauranteId);
        return mercadoPagoClient.criarPreferenceAssinatura(
            contrato.getPlano().getNome(), contrato.getPlano().getPrecoMensal(), contrato.getId());
    }

    // Chamado pelo webhook (PagamentoWebhookController) quando o Mercado Pago
    // notifica um pagamento. Sempre reconsulta o pagamento na API do MP em vez
    // de confiar no status que vier no corpo do webhook (recomendação oficial
    // do MP — o payload da notificação não é fonte de verdade).
    @Transactional
    @SuppressWarnings("null")
    public void confirmarPagamentoMercadoPago(String mpPaymentId) {
        if (pagamentoRepository.findByMpPaymentId(mpPaymentId).isPresent()) {
            return; // já processado — o MP reenvia notificações (idempotência)
        }
        Payment payment = mercadoPagoClient.buscarPagamento(Long.valueOf(mpPaymentId));
        if (!"approved".equals(payment.getStatus())) {
            return;
        }
        Long contratoId = Long.valueOf(payment.getExternalReference());
        Contrato contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new NoSuchElementException(
                "Contrato não encontrado para pagamento Mercado Pago " + mpPaymentId));

        pagamentoRepository.save(Pagamento.builder()
            .contrato(contrato)
            .valor(payment.getTransactionAmount())
            .status(StatusPagamento.PAGO)
            .dataPagamento(LocalDate.now())
            .metodo("MERCADO_PAGO")
            .mpPaymentId(mpPaymentId)
            .build());
        ativarContratoAposPagamento(contrato);
    }

    @SuppressWarnings("null")
    private void ativarContratoAposPagamento(Contrato contrato) {
        contrato.setStatus(StatusContrato.ATIVO);
        contrato.setDataVencimento(LocalDate.now().plusDays(30));
        contrato.setDataProximoVencimento(LocalDate.now().plusDays(30));
        contratoRepository.save(contrato);
        try { authInternalClient.desbloquear(contrato.getRestauranteId()); } catch (Exception e) {
            log.warn("Falha ao desbloquear restaurante {}: {}", contrato.getRestauranteId(), e.getMessage());
        }
    }

    // ─── Relatório financeiro ─────────────────────────────────────────────────

    public Map<String, Object> relatorioReceita(LocalDate inicio, LocalDate fim) {
        BigDecimal total = pagamentoRepository.somarReceitaPeriodo(inicio, fim);
        List<Pagamento> pagamentos = pagamentoRepository.findByStatus(StatusPagamento.PAGO);
        long inadimplentes = contratoRepository.findInadimplentes(LocalDate.now()).size();
        long bloqueados = contratoRepository.findByStatusOrderByCreatedAtDesc(StatusContrato.BLOQUEADO).size();
        return Map.of(
            "receitaTotal", total,
            "totalPagos", pagamentos.size(),
            "inadimplentes", inadimplentes,
            "bloqueados", bloqueados
        );
    }

    // ─── Bloqueio ─────────────────────────────────────────────────────────────

    @Transactional
    public void bloquearContratosPorInadimplencia() {
        LocalDate limite = LocalDate.now().minusDays(5);
        List<Contrato> vencidos = contratoRepository.findVencidosAntesde(limite);
        for (Contrato c : vencidos) {
            c.setStatus(StatusContrato.BLOQUEADO);
            contratoRepository.save(c);
            notificarBloqueio(c.getRestauranteId());
            log.info("Contrato #{} do restaurante {} bloqueado por inadimplência", c.getId(), c.getRestauranteId());
        }
    }

    private void notificarBloqueio(Long restauranteId) {
        try { authInternalClient.bloquear(restauranteId); } catch (Exception e) {
            log.warn("Falha ao bloquear restaurante {} no auth-service: {}", restauranteId, e.getMessage());
        }
    }
}
