package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.service.AssinaturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Notificações do Mercado Pago (Webhooks configurados no painel da
// aplicação, eventos "Planos e assinaturas"). Público no gateway e no
// SecurityConfig — a autenticidade vem da validação do x-signature.
@RestController
@RequestMapping("/api/pagamentos/webhook")
@RequiredArgsConstructor
@Slf4j
public class PagamentoWebhookController {

    private final AssinaturaService assinaturaService;

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestParam(name = "data.id", required = false) String dataIdParam,
            @RequestParam(name = "type", required = false) String typeParam,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId,
            @RequestBody(required = false) Map<String, Object> body) {

        String tipo = typeParam;
        String dataId = dataIdParam;
        if (body != null) {
            if (tipo == null && body.get("type") != null) tipo = body.get("type").toString();
            if (dataId == null && body.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
                dataId = data.get("id").toString();
            }
        }

        // O MP assina com o data.id da query string — é ele que entra no manifest.
        if (!assinaturaService.assinaturaWebhookValida(xSignature, xRequestId, dataIdParam)) {
            log.warn("Webhook MP com assinatura inválida (type={}, id={})", tipo, dataId);
            return ResponseEntity.status(401).build();
        }

        // Exceção aqui vira 500 de propósito: o MP retenta a notificação.
        assinaturaService.processarNotificacao(tipo, dataId);
        return ResponseEntity.ok().build();
    }
}
