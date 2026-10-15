package com.oiaaconta.order.service;

import com.oiaaconta.order.client.MercadoPagoApi;
import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.dto.NotificacaoMessage;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.entity.MercadoPagoConta;
import com.oiaaconta.order.enums.MetodoPagamento;
import com.oiaaconta.order.enums.StatusEntrega;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.exception.ResourceNotFoundException;
import com.oiaaconta.order.repository.EntregaRepository;
import com.oiaaconta.order.util.ItemPrecoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

// Pagamento online do pedido do cardápio digital — PIX (QR na tela) ou
// cartão de crédito (Card Payment Brick), criado com o access token da conta
// Mercado Pago do PRÓPRIO restaurante: o dinheiro cai direto nela.
//
// O pedido nasce em AGUARDANDO_PAGAMENTO e só entra no fluxo normal
// (AGUARDANDO → aceitar/rejeitar) quando o MP aprova. Confirmação por dois
// caminhos, ambos idempotentes e sempre revalidando direto na API do MP:
// webhook (notification_url de cada pagamento) e o polling do próprio
// cardápio enquanto o cliente espera o PIX cair.
@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoOnlineService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int MINUTOS_EXPIRACAO_PIX = 30;

    private final EntregaRepository entregaRepository;
    private final MercadoPagoContaService contaService;
    private final MercadoPagoApi mercadoPagoApi;
    private final NotificationClient notificationClient;

    // Base pública da API (ex: https://api.oiaaconta.com.br) pra montar o
    // notification_url de cada pagamento. Vazio = sem webhook (só polling).
    @Value("${mercadopago.api-public-url:}")
    private String apiPublicUrl;

    // ─── Cardápio (público) ──────────────────────────────────────────────────

    public Map<String, Object> configPublica(Long restauranteId) {
        Map<String, Object> r = new HashMap<>();
        contaService.contaConectada(restauranteId).ifPresentOrElse(conta -> {
            r.put("disponivel", true);
            r.put("publicKey", conta.getPublicKey());
        }, () -> r.put("disponivel", false));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumo(Long entregaId, String token) {
        Entrega entrega = buscarPorToken(entregaId, token);
        return resumo(entrega);
    }

    @Transactional
    public Map<String, Object> criarPagamento(Long entregaId, String token, Map<String, Object> dados) {
        Entrega entrega = buscarPorToken(entregaId, token);
        if (entrega.getStatus() != StatusEntrega.AGUARDANDO_PAGAMENTO) {
            throw new BusinessException(entrega.getStatus() == StatusEntrega.CANCELADA
                ? "Este pedido foi cancelado. Faça um novo pedido."
                : "Este pedido já foi pago.");
        }
        MercadoPagoConta conta = contaService.contaConectada(entrega.getRestauranteId())
            .orElseThrow(() -> new BusinessException("Este restaurante não aceita mais pagamento online. Faça um novo pedido."));

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", total(entrega));
        body.put("description", "Pedido #" + entrega.getId());
        body.put("external_reference", referencia(entrega));
        body.put("statement_descriptor", "PEDIDO " + entrega.getId());
        if (apiPublicUrl != null && !apiPublicUrl.isBlank()) {
            body.put("notification_url", apiPublicUrl.replaceAll("/+$", "")
                + "/api/pagamento-online/webhook?restaurante=" + entrega.getRestauranteId());
        }

        Map<String, Object> payer = new HashMap<>();
        String email = dados.get("payerEmail") instanceof String s && !s.isBlank()
            ? s.trim() : "pedido-" + entrega.getId() + "@cliente.oiaaconta.com.br";
        payer.put("email", email);
        if (entrega.getClienteNome() != null) payer.put("first_name", entrega.getClienteNome());
        body.put("payer", payer);

        if (entrega.getMetodoPagamento() == MetodoPagamento.PIX) {
            body.put("payment_method_id", "pix");
            body.put("date_of_expiration", LocalDateTime.now(FUSO).plusMinutes(MINUTOS_EXPIRACAO_PIX)
                .atZone(FUSO).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));
        } else {
            Object cardToken = dados.get("token");
            Object paymentMethodId = dados.get("paymentMethodId");
            if (cardToken == null || paymentMethodId == null) {
                throw new BusinessException("Dados do cartão incompletos.");
            }
            body.put("token", cardToken);
            body.put("payment_method_id", paymentMethodId);
            body.put("installments", 1);
            if (dados.get("issuerId") != null && !dados.get("issuerId").toString().isBlank()) {
                body.put("issuer_id", dados.get("issuerId"));
            }
            if (dados.get("identificationType") != null && dados.get("identificationNumber") != null) {
                payer.put("identification", Map.of(
                    "type", dados.get("identificationType"),
                    "number", dados.get("identificationNumber")));
            }
        }

        Map<String, Object> pagamento = mercadoPagoApi.post(contaService.accessToken(conta), "/v1/payments", body);
        String status = String.valueOf(pagamento.get("status"));
        entrega.setMpPaymentId(String.valueOf(pagamento.get("id")));
        entrega.setPagamentoOnlineStatus(status);
        entregaRepository.save(entrega);
        log.info("Pagamento online MP {} ({}) criado pro pedido #{}: {}", entrega.getMpPaymentId(),
            entrega.getMetodoPagamento(), entrega.getId(), status);

        if ("approved".equals(status)) {
            liberarPedido(entrega);
        }

        Map<String, Object> r = resumo(entrega);
        if (pagamento.get("point_of_interaction") instanceof Map<?, ?> poi
                && poi.get("transaction_data") instanceof Map<?, ?> td) {
            r.put("pixQrCode", td.get("qr_code"));
            r.put("pixQrCodeBase64", td.get("qr_code_base64"));
            r.put("pixExpiraEm", pagamento.get("date_of_expiration"));
        }
        if ("rejected".equals(status)) {
            r.put("motivo", motivoRecusa(String.valueOf(pagamento.get("status_detail"))));
        }
        return r;
    }

    // Polling do cardápio (PIX): se ainda não aprovado, consulta o MP na hora
    // — não depende do webhook ter chegado.
    @Transactional
    public Map<String, Object> consultarStatus(Long entregaId, String token) {
        Entrega entrega = buscarPorToken(entregaId, token);
        if (entrega.getStatus() == StatusEntrega.AGUARDANDO_PAGAMENTO && entrega.getMpPaymentId() != null) {
            sincronizar(entrega, entrega.getMpPaymentId());
        }
        return resumo(entrega);
    }

    // ─── Webhook ──────────────────────────────────────────────────────────────

    // Não confia no corpo da notificação: busca o pagamento na API do MP com
    // o token do restaurante informado e só aceita se o external_reference
    // apontar pra um pedido DESSE restaurante. Uma notificação forjada, no
    // máximo, faz a gente reconsultar um pagamento real.
    @Transactional
    public void processarWebhook(Long restauranteId, String paymentId) {
        if (restauranteId == null || paymentId == null || !paymentId.matches("\\d{1,30}")) return;
        MercadoPagoConta conta = contaService.contaConectada(restauranteId).orElse(null);
        if (conta == null) return;
        Map<String, Object> pagamento = mercadoPagoApi.get(contaService.accessToken(conta), "/v1/payments/" + paymentId);
        Long entregaId = entregaIdDaReferencia(pagamento.get("external_reference"));
        if (entregaId == null) return;
        entregaRepository.findByIdAndRestauranteId(entregaId, restauranteId)
            .ifPresent(entrega -> aplicar(entrega, conta, pagamento));
    }

    // ─── Reembolso ───────────────────────────────────────────────────────────

    // Pedido pago online e depois rejeitado/cancelado pelo restaurante:
    // devolve o valor integral pro cliente. Falha no reembolso NÃO impede o
    // cancelamento (o restaurante já decidiu) — fica logado pra resolver
    // manualmente no painel do MP.
    public void reembolsarSePago(Entrega entrega) {
        if (!Boolean.TRUE.equals(entrega.getPagamentoOnline()) || !"approved".equals(entrega.getPagamentoOnlineStatus())
                || entrega.getMpPaymentId() == null) {
            return;
        }
        try {
            MercadoPagoConta conta = contaService.contaConectada(entrega.getRestauranteId())
                .orElseThrow(() -> new BusinessException("Conta Mercado Pago desconectada"));
            mercadoPagoApi.post(contaService.accessToken(conta), "/v1/payments/" + entrega.getMpPaymentId() + "/refunds", Map.of());
            entrega.setPagamentoOnlineStatus("refunded");
            log.info("Pagamento online {} do pedido #{} reembolsado", entrega.getMpPaymentId(), entrega.getId());
        } catch (Exception e) {
            log.error("FALHA ao reembolsar pagamento online {} do pedido #{} — reembolsar manualmente no Mercado Pago: {}",
                entrega.getMpPaymentId(), entrega.getId(), e.getMessage());
        }
    }

    // Pedido que ficou esperando pagamento (cliente desistiu, PIX expirou)
    // vira CANCELADA — nunca chegou ao restaurante, então ninguém é avisado.
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 2 * 60 * 1000L)
    @Transactional
    public void expirarPendentes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_EXPIRACAO_PIX + 30);
        for (Entrega e : entregaRepository.findByStatusAndCreatedAtBefore(StatusEntrega.AGUARDANDO_PAGAMENTO, limite)) {
            if (e.getMpPaymentId() != null) {
                try { sincronizar(e, e.getMpPaymentId()); } catch (Exception ex) {
                    log.warn("Falha ao consultar pagamento do pedido #{} antes de expirar: {}", e.getId(), ex.getMessage());
                    continue;
                }
                if (e.getStatus() != StatusEntrega.AGUARDANDO_PAGAMENTO) continue;
            }
            e.setStatus(StatusEntrega.CANCELADA);
            e.setMotivoRejeicao("Pagamento online não concluído");
            entregaRepository.save(e);
        }
    }

    // ─── Internos ────────────────────────────────────────────────────────────

    private void sincronizar(Entrega entrega, String paymentId) {
        MercadoPagoConta conta = contaService.contaConectada(entrega.getRestauranteId()).orElse(null);
        if (conta == null) return;
        aplicar(entrega, conta, mercadoPagoApi.get(contaService.accessToken(conta), "/v1/payments/" + paymentId));
    }

    private void aplicar(Entrega entrega, MercadoPagoConta conta, Map<String, Object> pagamento) {
        if (!referencia(entrega).equals(String.valueOf(pagamento.get("external_reference")))) return;
        String paymentId = String.valueOf(pagamento.get("id"));
        String status = String.valueOf(pagamento.get("status"));

        if (entrega.getStatus() == StatusEntrega.AGUARDANDO_PAGAMENTO) {
            // Só o pagamento mais recente define o status exibido, exceto
            // aprovação: qualquer pagamento aprovado do pedido o libera (ex:
            // cliente gerou dois PIX e pagou o primeiro).
            if ("approved".equals(status)) {
                if (!valorConfere(entrega, pagamento)) {
                    log.error("Pagamento {} aprovado com valor diferente do pedido #{} — não liberado", paymentId, entrega.getId());
                    return;
                }
                entrega.setMpPaymentId(paymentId);
                entrega.setPagamentoOnlineStatus(status);
                liberarPedido(entrega);
            } else if (paymentId.equals(entrega.getMpPaymentId())) {
                entrega.setPagamentoOnlineStatus(status);
                entregaRepository.save(entrega);
            }
            return;
        }

        // Pedido já pago e chegou a aprovação de OUTRO pagamento (cliente
        // pagou dois PIX): devolve o duplicado.
        if ("approved".equals(status) && !paymentId.equals(entrega.getMpPaymentId())
                && "approved".equals(entrega.getPagamentoOnlineStatus())) {
            try {
                mercadoPagoApi.post(contaService.accessToken(conta), "/v1/payments/" + paymentId + "/refunds", Map.of());
                log.info("Pagamento duplicado {} do pedido #{} reembolsado", paymentId, entrega.getId());
            } catch (Exception e) {
                log.error("FALHA ao reembolsar pagamento duplicado {} do pedido #{}: {}", paymentId, entrega.getId(), e.getMessage());
            }
        } else if (paymentId.equals(entrega.getMpPaymentId()) && !status.equals(entrega.getPagamentoOnlineStatus())) {
            // Mudança depois de aprovado (estorno/chargeback feito no MP).
            entrega.setPagamentoOnlineStatus(status);
            entregaRepository.save(entrega);
        }
    }

    // Entra no fluxo normal de pedido do cliente: AGUARDANDO + aviso pro
    // restaurante. PIX já pago dispensa a validação manual do PIX (ver
    // EntregaService.confirmar).
    private void liberarPedido(Entrega entrega) {
        entrega.setStatus(StatusEntrega.AGUARDANDO);
        if (entrega.getMetodoPagamento() == MetodoPagamento.PIX) {
            entrega.setPagamentoPixValidado(true);
        }
        Entrega saved = entregaRepository.save(entrega);
        log.info("Pedido #{} pago online — liberado pro restaurante {}", saved.getId(), saved.getRestauranteId());
        try {
            notificationClient.novaEntregaAguardando(NotificacaoMessage.builder()
                .tipo("NOVA_ENTREGA_AGUARDANDO")
                .pedidoId(saved.getId())
                .restauranteId(saved.getRestauranteId())
                .mensagem("Novo pedido (pago online) de " + saved.getClienteNome())
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar nova entrega paga online #{}: {}", saved.getId(), e.getMessage());
        }
    }

    private Map<String, Object> resumo(Entrega entrega) {
        Map<String, Object> r = new HashMap<>();
        r.put("entregaId", entrega.getId());
        r.put("total", total(entrega));
        r.put("metodo", entrega.getMetodoPagamento().name());
        r.put("pago", entrega.getStatus() != StatusEntrega.AGUARDANDO_PAGAMENTO && entrega.getStatus() != StatusEntrega.CANCELADA
            && "approved".equals(entrega.getPagamentoOnlineStatus()));
        r.put("cancelado", entrega.getStatus() == StatusEntrega.CANCELADA);
        r.put("status", entrega.getPagamentoOnlineStatus());
        return r;
    }

    private BigDecimal total(Entrega entrega) {
        BigDecimal itens = entrega.getItens().stream()
            .map(i -> ItemPrecoUtil.subtotal(i.getPrecoUnitario(), i.getQuantidade(), i.getPesoGramas(), i.getVolumeMililitros()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal frete = entrega.getValorFrete() != null ? entrega.getValorFrete() : BigDecimal.ZERO;
        return itens.add(frete).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean valorConfere(Entrega entrega, Map<String, Object> pagamento) {
        Object valor = pagamento.get("transaction_amount");
        return valor != null && new BigDecimal(valor.toString()).compareTo(total(entrega)) >= 0;
    }

    private Entrega buscarPorToken(Long entregaId, String token) {
        Entrega entrega = entregaRepository.findById(entregaId)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        if (!Boolean.TRUE.equals(entrega.getPagamentoOnline()) || entrega.getPagamentoOnlineToken() == null || token == null
                || !MessageDigest.isEqual(entrega.getPagamentoOnlineToken().getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResourceNotFoundException("Pedido não encontrado");
        }
        return entrega;
    }

    private static String referencia(Entrega entrega) {
        return "entrega-" + entrega.getId();
    }

    private static Long entregaIdDaReferencia(Object ref) {
        if (ref == null) return null;
        String s = ref.toString();
        if (!s.startsWith("entrega-")) return null;
        try { return Long.parseLong(s.substring("entrega-".length())); } catch (NumberFormatException e) { return null; }
    }

    // status_detail do MP → texto pro cliente (os mais comuns de cartão).
    private static String motivoRecusa(String detalhe) {
        return switch (detalhe) {
            case "cc_rejected_insufficient_amount" -> "Saldo/limite insuficiente.";
            case "cc_rejected_bad_filled_security_code" -> "Código de segurança inválido.";
            case "cc_rejected_bad_filled_date" -> "Data de validade inválida.";
            case "cc_rejected_bad_filled_other", "cc_rejected_bad_filled_card_number" -> "Confira os dados do cartão.";
            case "cc_rejected_call_for_authorize" -> "O banco pediu autorização — ligue para o emissor do cartão.";
            case "cc_rejected_card_disabled" -> "Cartão desabilitado — fale com o emissor.";
            case "cc_rejected_duplicated_payment" -> "Pagamento duplicado — já existe um pagamento igual recente.";
            case "cc_rejected_high_risk" -> "Pagamento recusado pela análise de segurança. Tente outro cartão ou PIX.";
            default -> "Pagamento recusado. Tente outro cartão ou pague com PIX.";
        };
    }
}
