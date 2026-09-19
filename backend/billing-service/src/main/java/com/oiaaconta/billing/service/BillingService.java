package com.oiaaconta.billing.service;

import com.oiaaconta.billing.client.AuthInternalClient;
import com.oiaaconta.billing.dto.response.RestricoesOperacaoResponse;
import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.entity.LinkSocial;
import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.entity.Plano;
import com.oiaaconta.billing.enums.ModalidadeOperacao;
import com.oiaaconta.billing.enums.StatusContrato;
import com.oiaaconta.billing.enums.StatusPagamento;
import com.oiaaconta.billing.exception.LimiteTrocasModalidadeExcedidoException;
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

    // Trocas gratuitas de modalidade por contrato — da 3ª em diante só o
    // suporte pode trocar, e cada troca cobra VALOR_ENCARGO_TROCA.
    private static final int LIMITE_TROCAS_GRATIS = 2;
    private static final BigDecimal VALOR_ENCARGO_TROCA = new BigDecimal("30.00");

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
        plano.setFuncionalidadesMesas(dados.getFuncionalidadesMesas());
        plano.setFuncionalidadesDelivery(dados.getFuncionalidadesDelivery());
        plano.setAtivo(dados.isAtivo());
        plano.setDestaque(dados.isDestaque());
        plano.setExigeModalidadeOperacao(dados.isExigeModalidadeOperacao());
        plano.setLimiteAtendentesWhatsapp(dados.getLimiteAtendentesWhatsapp());
        plano.setLimiteAtendentesWhatsappMesas(dados.getLimiteAtendentesWhatsappMesas());
        plano.setLimiteAtendentesWhatsappDelivery(dados.getLimiteAtendentesWhatsappDelivery());
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
    public Contrato criarContrato(Long restauranteId, Long planoId, ModalidadeOperacao modalidadeOperacao) {
        if (restauranteId == null || planoId == null) {
            throw new IllegalArgumentException("restauranteId e planoId são obrigatórios");
        }
        if (contratoRepository.findByRestauranteId(restauranteId).isPresent()) {
            throw new IllegalStateException("Restaurante já possui contrato");
        }
        Plano plano = planoRepository.findById(planoId)
            .orElseThrow(() -> new NoSuchElementException("Plano não encontrado"));
        if (plano.isExigeModalidadeOperacao() && modalidadeOperacao == null) {
            throw new IllegalArgumentException("Este plano exige a escolha da modalidade de operação (mesas ou delivery)");
        }
        LocalDate hoje = LocalDate.now();
        return contratoRepository.save(Contrato.builder()
            .restauranteId(restauranteId)
            .plano(plano)
            .status(StatusContrato.TRIAL)
            .dataInicio(hoje)
            .dataVencimento(hoje.plusDays(30))
            .dataProximoVencimento(hoje.plusDays(30))
            .modalidadeOperacao(plano.isExigeModalidadeOperacao() ? modalidadeOperacao : null)
            .build());
    }

    // Troca de modalidade depois da criação do contrato — usada pelo suporte
    // (SUPER_ADMIN) quando o dono pede pra mudar de mesas pra delivery ou
    // vice-versa. Só se aplica a planos que exigem modalidade; nos demais é
    // sempre null e essa troca não faz sentido. Suporte sempre pode trocar
    // (sem bloqueio de limite) — só passa a cobrar depois das 2 gratuitas.
    @Transactional
    @SuppressWarnings("null")
    public Contrato atualizarModalidadeOperacao(Long contratoId, ModalidadeOperacao modalidadeOperacao) {
        Contrato contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado"));
        validarPlanoExigeModalidade(contrato);
        if (modalidadeOperacao == null) {
            throw new IllegalArgumentException("modalidadeOperacao é obrigatória");
        }
        aplicarTrocaModalidade(contrato, modalidadeOperacao);
        return contratoRepository.save(contrato);
    }

    // Troca de modalidade feita pelo próprio dono do restaurante, direto no
    // painel (Configurações) — só permitida enquanto ainda tiver trocas
    // gratuitas disponíveis. Depois disso, precisa pedir ao suporte (ver
    // atualizarModalidadeOperacao acima), que já embute a cobrança.
    @Transactional
    @SuppressWarnings("null")
    public Contrato alterarMinhaModalidade(Long restauranteId, ModalidadeOperacao modalidadeOperacao) {
        Contrato contrato = buscarContratoDoRestaurante(restauranteId);
        validarPlanoExigeModalidade(contrato);
        if (modalidadeOperacao == null) {
            throw new IllegalArgumentException("modalidadeOperacao é obrigatória");
        }
        if (contrato.getTrocasModalidadeGratisUsadas() >= LIMITE_TROCAS_GRATIS
                && modalidadeOperacao != contrato.getModalidadeOperacao()) {
            throw new LimiteTrocasModalidadeExcedidoException(
                "Você já usou as " + LIMITE_TROCAS_GRATIS + " trocas gratuitas de modalidade. " +
                "Para trocar novamente, solicite ao suporte — cada troca custa R$ " + VALOR_ENCARGO_TROCA +
                ", cobrada na sua próxima fatura.");
        }
        aplicarTrocaModalidade(contrato, modalidadeOperacao);
        return contratoRepository.save(contrato);
    }

    private void validarPlanoExigeModalidade(Contrato contrato) {
        if (!contrato.getPlano().isExigeModalidadeOperacao()) {
            throw new IllegalStateException("O plano deste contrato não usa modalidade de operação");
        }
    }

    // Aplica a troca em si: sem custo enquanto não estourar o limite de
    // trocas gratuitas; a partir daí soma VALOR_ENCARGO_TROCA no saldo de
    // encargos do contrato (cobrado depois — ver registrarPagamentoManual).
    // Trocar pra modalidade igual à atual não conta como troca.
    private void aplicarTrocaModalidade(Contrato contrato, ModalidadeOperacao nova) {
        if (nova == contrato.getModalidadeOperacao()) {
            return;
        }
        contrato.setModalidadeOperacao(nova);
        if (contrato.getTrocasModalidadeGratisUsadas() < LIMITE_TROCAS_GRATIS) {
            contrato.setTrocasModalidadeGratisUsadas(contrato.getTrocasModalidadeGratisUsadas() + 1);
        } else {
            contrato.setSaldoEncargosModalidade(contrato.getSaldoEncargosModalidade().add(VALOR_ENCARGO_TROCA));
        }
    }

    // Troca de plano feita pelo próprio dono — sem cálculo de cobrança
    // proporcional (o contrato de adesão prevê "ajuste no próximo
    // vencimento", mas isso ainda não é automatizado no sistema; fica pro
    // suporte conferir manualmente se precisar). Se o novo plano exige
    // modalidade, é obrigatório informar qual.
    @Transactional
    @SuppressWarnings("null")
    public Contrato alterarMeuPlano(Long restauranteId, Long novoPlanoId, ModalidadeOperacao modalidadeOperacao) {
        Contrato contrato = buscarContratoDoRestaurante(restauranteId);
        Plano novoPlano = planoRepository.findById(novoPlanoId)
            .orElseThrow(() -> new NoSuchElementException("Plano não encontrado"));
        if (novoPlano.isExigeModalidadeOperacao() && modalidadeOperacao == null) {
            throw new IllegalArgumentException("Este plano exige a escolha da modalidade de operação (mesas ou delivery)");
        }
        contrato.setPlano(novoPlano);
        contrato.setModalidadeOperacao(novoPlano.isExigeModalidadeOperacao() ? modalidadeOperacao : null);
        return contratoRepository.save(contrato);
    }

    // Resolve, num valor só por restrição, o que os outros serviços
    // (table/order/ifood/auth) precisam saber sem replicar a lógica de
    // modalidade cada um por conta própria.
    public RestricoesOperacaoResponse buscarRestricoesOperacao(Long restauranteId) {
        Contrato contrato = buscarContratoDoRestaurante(restauranteId);
        Plano plano = contrato.getPlano();
        boolean restringe = plano.isExigeModalidadeOperacao();
        ModalidadeOperacao modalidade = contrato.getModalidadeOperacao();

        Integer limiteWhatsapp;
        boolean permiteIfood;
        if (!restringe) {
            limiteWhatsapp = plano.getLimiteAtendentesWhatsapp();
            permiteIfood = true;
        } else if (modalidade == ModalidadeOperacao.DELIVERY) {
            limiteWhatsapp = plano.getLimiteAtendentesWhatsappDelivery();
            permiteIfood = true;
        } else {
            // MESAS, ou (não deveria acontecer) restringe sem modalidade
            // definida ainda — trata como o modo mais restrito, presencial.
            limiteWhatsapp = plano.getLimiteAtendentesWhatsappMesas();
            permiteIfood = false;
        }
        return new RestricoesOperacaoResponse(restringe, modalidade, plano.getLimiteMesas(), limiteWhatsapp, permiteIfood);
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

        // Soma automaticamente eventuais encargos pendentes de trocas de
        // modalidade além do limite gratuito — é o único ponto do sistema
        // onde uma cobrança é de fato gerada pra esse contrato, então é
        // aqui que o "cobrado na próxima fatura" acontece de verdade.
        BigDecimal valorFinal = valor;
        String observacaoFinal = observacao;
        BigDecimal encargos = contrato.getSaldoEncargosModalidade();
        if (encargos != null && encargos.compareTo(BigDecimal.ZERO) > 0) {
            valorFinal = valor.add(encargos);
            String notaEncargo = "+ R$ " + encargos + " de encargos por troca de modalidade além do limite gratuito";
            observacaoFinal = (observacao == null || observacao.isBlank())
                ? notaEncargo
                : observacao + " (" + notaEncargo + ")";
            contrato.setSaldoEncargosModalidade(BigDecimal.ZERO);
        }

        Pagamento pag = pagamentoRepository.save(Pagamento.builder()
            .contrato(contrato)
            .valor(valorFinal)
            .status(StatusPagamento.PAGO)
            .dataPagamento(LocalDate.now())
            .metodo("MANUAL")
            .observacao(observacaoFinal)
            .build());
        contrato.setStatus(StatusContrato.ATIVO);
        contrato.setDataVencimento(LocalDate.now().plusDays(30));
        contrato.setDataProximoVencimento(LocalDate.now().plusDays(30));
        contratoRepository.save(contrato);
        try { authInternalClient.desbloquear(contrato.getRestauranteId()); } catch (Exception e) {
            log.warn("Falha ao desbloquear restaurante {}: {}", contrato.getRestauranteId(), e.getMessage());
        }
        return pag;
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
