package com.oiaaconta.whatsapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.whatsapp.client.AuthClient;
import com.oiaaconta.whatsapp.client.BillingClient;
import com.oiaaconta.whatsapp.client.EvolutionApiClient.WebhookPayload;
import com.oiaaconta.whatsapp.service.ChatbotService;
import com.oiaaconta.whatsapp.service.MensagemWhatsappService;
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
    private final MensagemWhatsappService mensagemWhatsappService;
    private final ObjectMapper objectMapper;

    @Value("${evolution.webhook.secret:}")
    private String webhookSecret;

    // Instância dedicada da linha de suporte da plataforma — NÃO é a mesma
    // property de evolution.api.instance (essa é usada pelo
    // EvolutionApiClient pra enviar mensagem e hoje aponta pro único número
    // conectado, que é de um restaurante). Sem valor configurado (vazio),
    // nunca bate com o nome de instância de nenhum restaurante, então
    // nenhuma mensagem cai indevidamente no fluxo de ticket.
    @Value("${evolution.plataforma.instance:}")
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
            boolean fromMe = data.getKey().isFromMe();

            String telefone = data.getKey().getRemoteJid();
            if (telefone == null) return ResponseEntity.ok().build();
            if (telefone.endsWith("@g.us")) {
                log.info("JID ignorado (grupo): {}", telefone);
                return ResponseEntity.ok().build();
            }
            // @lid: novo formato multi-device — mantém o JID completo como identificador
            String numeroReal = null;
            if (!telefone.endsWith("@lid")) {
                telefone = telefone.replace("@s.whatsapp.net", "");
            } else {
                // O WhatsApp manda o número de verdade junto, em remoteJidAlt —
                // sem isso o chatbot teria que perguntar o número ao cliente
                // mesmo quando ele já veio pronto (ver ChatbotService).
                String alt = data.getKey().getRemoteJidAlt();
                if (alt != null && alt.endsWith("@s.whatsapp.net")) {
                    numeroReal = alt.replace("@s.whatsapp.net", "");
                }
            }

            String texto = data.getMessage() != null ? data.getMessage().getText() : null;
            if (texto == null || texto.isBlank()) return ResponseEntity.ok().build();

            String pushName = data.getPushName();

            if (StringUtils.hasText(instanciaPlataforma) && instanciaPlataforma.equals(payload.getInstance())) {
                if (fromMe) return ResponseEntity.ok().build();
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

            if (fromMe) {
                // Mensagem enviada pelo restaurante — pelo painel/bot (já
                // registrada na hora do envio) ou direto do celular, fora do
                // sistema. Sem isso, uma resposta mandada direto do WhatsApp
                // do celular nunca aparecia no histórico de Conversas.
                mensagemWhatsappService.registrarEnviadaSeNova(restauranteId, telefone, texto);
                return ResponseEntity.ok().build();
            }

            chatbotService.processarMensagem(telefone, texto, restauranteId, pushName, numeroReal);

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
