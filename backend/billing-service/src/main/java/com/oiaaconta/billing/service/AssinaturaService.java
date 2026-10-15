package com.oiaaconta.billing.service;

import com.oiaaconta.billing.client.AuthInternalClient;
import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.entity.Plano;
import com.oiaaconta.billing.enums.StatusContrato;
import com.oiaaconta.billing.enums.StatusPagamento;
import com.oiaaconta.billing.exception.MercadoPagoException;
import com.oiaaconta.billing.repository.ContratoRepository;
import com.oiaaconta.billing.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.NoSuchElementException;

// Cobrança recorrente no cartão via Mercado Pago Assinaturas (preapproval
// sem plano associado, status "authorized"). O cartão é tokenizado no
// frontend (Card Payment Brick, public key) e só o token chega aqui — o
// número do cartão nunca passa pelo nosso backend.
//
// Usa a API REST direto em vez do SDK: o sdk-java 2.1.14 não tem
// card_token_id no PreapprovalCreateRequest, então o SDK só consegue criar
// assinatura "pending" (com redirecionamento pro checkout do MP).
@Service
@RequiredArgsConstructor
@Slf4j
public class AssinaturaService {

    private static final String MP_API = "https://api.mercadopago.com";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    private final ContratoRepository contratoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AuthInternalClient authInternalClient;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${mercadopago.frontend-base-url}")
    private String frontendBaseUrl;

    public String getPublicKey() {
        return publicKey;
    }

    // Primeira cobrança: fim do período de teste do plano, contado do início
    // do contrato (não de hoje) — quem cadastra o cartão no dia 10 do trial
    // continua sendo cobrado no dia 30. Plano sem período de teste (ex: o
    // plano "Teste" de R$ 0,50) ou trial já vencido: cobra na hora.
    public LocalDate dataPrimeiraCobranca(Contrato contrato) {
        Plano plano = contrato.getPlano();
        int dias = Boolean.TRUE.equals(plano.getPeriodoTeste()) && plano.getDiasTeste() != null
            ? plano.getDiasTeste() : 0;
        LocalDate data = contrato.getDataInicio().plusDays(dias);
        LocalDate hoje = LocalDate.now(FUSO);
        return data.isAfter(hoje) ? data : hoje;
    }

    @Transactional
    public Contrato criarAssinatura(Long restauranteId, String cardTokenId, String payerEmail) {
        if (cardTokenId == null || cardTokenId.isBlank() || payerEmail == null || payerEmail.isBlank()) {
            throw new IllegalArgumentException("Token do cartão e e-mail do pagador são obrigatórios");
        }
        Contrato contrato = contratoRepository.findByRestauranteId(restauranteId)
            .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado"));
        if (contrato.getMpPreapprovalId() != null) {
            throw new IllegalStateException("Este restaurante já tem um cartão cadastrado na assinatura");
        }

        Plano plano = contrato.getPlano();
        LocalDate primeiraCobranca = dataPrimeiraCobranca(contrato);
        boolean cobraAgora = !primeiraCobranca.isAfter(LocalDate.now(FUSO));

        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", plano.getPrecoMensal());
        autoRecurring.put("currency_id", "BRL");
        // Sem start_date o MP faz a primeira cobrança logo após a criação
        // (até ~1h depois). Com start_date futura, só cobra nessa data.
        if (!cobraAgora) {
            autoRecurring.put("start_date", primeiraCobranca.atTime(LocalTime.of(10, 0))
                .atZone(FUSO).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("reason", "Oia a Conta — Plano " + plano.getNome());
        body.put("external_reference", "contrato-" + contrato.getId());
        body.put("payer_email", payerEmail.trim());
        body.put("card_token_id", cardTokenId);
        body.put("status", "authorized");
        body.put("back_url", frontendBaseUrl + "/admin/assinatura");
        body.put("auto_recurring", autoRecurring);

        Map<String, Object> preapproval = chamarMp("POST", "/preapproval", body);
        String preapprovalId = String.valueOf(preapproval.get("id"));
        log.info("Assinatura MP {} criada pro contrato #{} (status {}, 1ª cobrança {})",
            preapprovalId, contrato.getId(), preapproval.get("status"), cobraAgora ? "imediata" : primeiraCobranca);

        contrato.setMpPreapprovalId(preapprovalId);
        if (!cobraAgora) {
            // O trial vai até a primeira cobrança — sem isso o
            // BloqueioScheduler bloquearia pelo vencimento antigo.
            contrato.setDataVencimento(primeiraCobranca);
            contrato.setDataProximoVencimento(primeiraCobranca);
        }
        return contratoRepository.save(contrato);
    }

    // ─── Webhook ──────────────────────────────────────────────────────────────

    // Validação da assinatura x-signature do webhook (HMAC-SHA256 com a
    // "assinatura secreta" configurada no painel do MP). Manifest:
    // "id:{data.id};request-id:{x-request-id};ts:{ts};" — partes ausentes
    // são omitidas, e data.id vai em minúsculas.
    public boolean assinaturaWebhookValida(String xSignature, String xRequestId, String dataId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("MP_WEBHOOK_SECRET não configurado — rejeitando notificação do Mercado Pago");
            return false;
        }
        if (xSignature == null) return false;
        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("ts")) ts = kv[1];
            if (kv[0].equals("v1")) v1 = kv[1];
        }
        if (ts == null || v1 == null) return false;

        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) manifest.append("id:").append(dataId.toLowerCase()).append(';');
        if (xRequestId != null && !xRequestId.isBlank()) manifest.append("request-id:").append(xRequestId).append(';');
        manifest.append("ts:").append(ts).append(';');

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperado = HexFormat.of().formatHex(mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Erro validando assinatura do webhook do MP: {}", e.getMessage());
            return false;
        }
    }

    // Nunca confia no corpo da notificação — sempre busca o recurso direto
    // na API do MP pelo id. Idempotente: o MP reenvia a mesma notificação
    // várias vezes, e uma cobrança recusada é retentada (recycling) gerando
    // notificações novas pro mesmo authorized_payment.
    @Transactional
    public void processarNotificacao(String tipo, String dataId) {
        if (tipo == null || dataId == null) return;
        switch (tipo) {
            case "subscription_authorized_payment" -> processarCobranca(dataId);
            case "subscription_preapproval" -> processarMudancaAssinatura(dataId);
            default -> log.debug("Notificação MP ignorada: type={} id={}", tipo, dataId);
        }
    }

    @SuppressWarnings("unchecked")
    private void processarCobranca(String authorizedPaymentId) {
        Map<String, Object> cobranca = chamarMp("GET", "/authorized_payments/" + authorizedPaymentId, null);
        String preapprovalId = String.valueOf(cobranca.get("preapproval_id"));
        Contrato contrato = contratoRepository.findByMpPreapprovalId(preapprovalId).orElse(null);
        if (contrato == null) {
            log.warn("Cobrança MP {} de assinatura desconhecida ({})", authorizedPaymentId, preapprovalId);
            return;
        }
        // Cobrança agendada que ainda não foi tentada não tem "payment".
        if (!(cobranca.get("payment") instanceof Map<?, ?> payment) || payment.get("id") == null) {
            return;
        }
        String mpPaymentId = String.valueOf(payment.get("id"));
        String statusMp = String.valueOf(payment.get("status"));
        String detalhe = String.valueOf(payment.get("status_detail"));

        Pagamento pagamento = pagamentoRepository.findByMpPaymentId(mpPaymentId).orElse(null);
        if (pagamento != null && pagamento.getStatus() == StatusPagamento.PAGO) {
            return;
        }
        if (pagamento == null) {
            pagamento = Pagamento.builder()
                .contrato(contrato)
                .mpPaymentId(mpPaymentId)
                .metodo("CARTAO_RECORRENTE")
                .build();
        }
        Object valor = cobranca.get("transaction_amount");
        pagamento.setValor(valor != null ? new BigDecimal(valor.toString()) : contrato.getPlano().getPrecoMensal());

        switch (statusMp) {
            case "approved" -> {
                LocalDate hoje = LocalDate.now(FUSO);
                pagamento.setStatus(StatusPagamento.PAGO);
                pagamento.setDataPagamento(hoje);
                pagamento.setObservacao(null);
                pagamentoRepository.save(pagamento);

                boolean estavaBloqueado = contrato.getStatus() == StatusContrato.BLOQUEADO;
                contrato.setStatus(StatusContrato.ATIVO);
                contrato.setDataVencimento(hoje.plusMonths(1));
                contrato.setDataProximoVencimento(hoje.plusMonths(1));
                contratoRepository.save(contrato);
                if (estavaBloqueado) {
                    try { authInternalClient.desbloquear(contrato.getRestauranteId()); } catch (Exception e) {
                        log.warn("Falha ao desbloquear restaurante {}: {}", contrato.getRestauranteId(), e.getMessage());
                    }
                }
                log.info("Cobrança MP {} aprovada — contrato #{} ativo até {}", mpPaymentId, contrato.getId(), contrato.getDataVencimento());
            }
            case "rejected", "cancelled" -> {
                pagamento.setStatus(StatusPagamento.FALHOU);
                pagamento.setObservacao("Cartão recusado (" + detalhe + ")");
                pagamentoRepository.save(pagamento);
                log.info("Cobrança MP {} recusada pro contrato #{}: {}", mpPaymentId, contrato.getId(), detalhe);
            }
            default -> {
                pagamento.setStatus(StatusPagamento.PENDENTE);
                pagamento.setObservacao("MP: " + statusMp + " (" + detalhe + ")");
                pagamentoRepository.save(pagamento);
            }
        }
    }

    // Assinatura cancelada (pelo dono no app do MP, pelo suporte ou pelo
    // próprio MP depois de esgotar as retentativas): desvincula do contrato,
    // assim a tela "Minha Assinatura" volta a pedir um cartão. O contrato em
    // si segue o fluxo normal de vencimento/bloqueio.
    private void processarMudancaAssinatura(String preapprovalId) {
        Map<String, Object> preapproval = chamarMp("GET", "/preapproval/" + preapprovalId, null);
        String status = String.valueOf(preapproval.get("status"));
        contratoRepository.findByMpPreapprovalId(preapprovalId).ifPresent(contrato -> {
            log.info("Assinatura MP {} do contrato #{} agora está '{}'", preapprovalId, contrato.getId(), status);
            if ("cancelled".equals(status)) {
                contrato.setMpPreapprovalId(null);
                contratoRepository.save(contrato);
            }
        });
    }

    // ─── HTTP ─────────────────────────────────────────────────────────────────

    private Map<String, Object> chamarMp(String metodo, String path, Object body) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new MercadoPagoException("Pagamento por cartão indisponível no momento (Mercado Pago não configurado).");
        }
        RestClient client = RestClient.builder()
            .baseUrl(MP_API)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        try {
            Map<String, Object> resposta = "POST".equals(metodo)
                ? client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JSON)
                : client.get().uri(path).retrieve().body(JSON);
            return resposta != null ? resposta : Map.of();
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago {} {} → {}: {}", metodo, path, e.getStatusCode().value(), e.getResponseBodyAsString());
            Map<String, Object> erro = e.getResponseBodyAs(JSON);
            Object msg = erro != null ? erro.get("message") : null;
            throw new MercadoPagoException("Não foi possível cadastrar o cartão no Mercado Pago"
                + (msg != null ? ": " + msg : "."));
        }
    }
}
