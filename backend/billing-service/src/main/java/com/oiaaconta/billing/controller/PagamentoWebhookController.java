package com.oiaaconta.billing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.billing.service.BillingService;
import com.oiaaconta.billing.util.MercadoPagoSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

// Endpoint público (fora de JWT — liberado em SecurityConfig e no
// api-gateway) chamado pelo Mercado Pago a cada evento de pagamento.
@RestController
@RequestMapping("/api/pagamentos/webhook")
@RequiredArgsConstructor
@Slf4j
public class PagamentoWebhookController {

    private final BillingService billingService;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataIdQuery,
            @RequestBody(required = false) String rawBody) {
        try {
            String dataId = dataIdQuery;
            String tipo = null;
            if (StringUtils.hasText(rawBody)) {
                JsonNode node = objectMapper.readTree(rawBody);
                tipo = node.path("type").asText(null);
                if (dataId == null) {
                    dataId = node.path("data").path("id").asText(null);
                }
            }
            if (dataId == null) {
                return ResponseEntity.ok().build();
            }

            if (StringUtils.hasText(webhookSecret)
                    && !MercadoPagoSignature.valida(xSignature, xRequestId, dataId, webhookSecret)) {
                log.warn("Webhook Mercado Pago rejeitado: assinatura inválida");
                return ResponseEntity.status(401).build();
            }

            if (tipo == null || "payment".equals(tipo)) {
                billingService.confirmarPagamentoMercadoPago(dataId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar webhook Mercado Pago: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }
}
