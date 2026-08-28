package com.oiaaconta.whatsapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.whatsapp.client.AuthClient;
import com.oiaaconta.whatsapp.client.BillingClient;
import com.oiaaconta.whatsapp.client.EvolutionApiClient.WebhookPayload;
import com.oiaaconta.whatsapp.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ChatbotService chatbotService;
    private final AuthClient authClient;
    private final BillingClient billingClient;
    private final ObjectMapper objectMapper;

    @Value("${evolution.webhook.secret:}")
    private String webhookSecret;

    // Mesmo padrão usado por WhatsappAdminService.resolverInstancia quando
    // restauranteId é null — é a instância da plataforma (suporte), não de um
    // restaurante específico.
    @Value("${evolution.api.instance:oiaaconta}")
    private String instanciaPlataforma;

    @PostMapping("/evolution")
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "X-Evolution-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (StringUtils.hasText(webhookSecret) && !validarAssinatura(signature, rawBody)) {
            log.warn("Webhook Evolution rejeitado: assinatura HMAC inválida");
            return ResponseEntity.status(401).build();
        }

        try {
            WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);

            if (!"messages.upsert".equals(payload.getEvent())) {
                return ResponseEntity.ok().build();
            }

            WebhookPayload.DataPayload data = payload.getData();
            if (data == null || data.getKey() == null) return ResponseEntity.ok().build();
            if (data.getKey().isFromMe()) return ResponseEntity.ok().build();

            String telefone = data.getKey().getRemoteJid();
            if (telefone == null) return ResponseEntity.ok().build();
            if (telefone.endsWith("@g.us")) {
                log.info("JID ignorado (grupo): {}", telefone);
                return ResponseEntity.ok().build();
            }
            // @lid: novo formato multi-device — mantém o JID completo como identificador
            if (!telefone.endsWith("@lid")) {
                telefone = telefone.replace("@s.whatsapp.net", "");
            }

            String texto = data.getMessage() != null ? data.getMessage().getText() : null;
            if (texto == null || texto.isBlank()) return ResponseEntity.ok().build();

            String pushName = data.getPushName();

            if (instanciaPlataforma.equals(payload.getInstance())) {
                try {
                    billingClient.registrarMensagemTicket(Map.of(
                        "telefone", telefone,
                        "nomeContato", pushName != null ? pushName : "",
                        "mensagem", texto));
                } catch (Exception e) {
                    log.error("Erro ao registrar ticket de suporte via WhatsApp: {}", e.getMessage());
                }
                return ResponseEntity.ok().build();
            }

            Long restauranteId = resolverRestauranteId(payload.getInstance());
            if (restauranteId == null) {
                log.warn("Instância '{}' não mapeada para nenhum restaurante — mensagem ignorada", payload.getInstance());
                return ResponseEntity.ok().build();
            }

            chatbotService.processarMensagem(telefone, texto, restauranteId, pushName);

        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private boolean validarAssinatura(String signature, String body) {
        if (!StringUtils.hasText(signature)) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Erro ao validar assinatura HMAC: {}", e.getMessage());
            return false;
        }
    }

    private Long resolverRestauranteId(String instanceName) {
        if (instanceName == null || instanceName.isBlank()) return null;
        try {
            AuthClient.RestauranteInfo info = authClient.findByInstanceName(instanceName);
            if (info == null || !info.isAtivo() || info.isBloqueado()) return null;
            return info.getRestauranteId();
        } catch (Exception e) {
            log.warn("Erro ao resolver restaurante para instância '{}': {}", instanceName, e.getMessage());
            return null;
        }
    }
}
