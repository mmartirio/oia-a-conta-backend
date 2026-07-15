package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.client.AuthClient;
import com.oiaaconta.whatsapp.dto.WhatsappStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class WhatsappAdminService {

    private final RestTemplate restTemplate;
    private final AuthClient authClient;

    @Value("${evolution.api.url}")
    private String apiUrl;

    @Value("${evolution.api.key}")
    private String apiKey;

    @Value("${evolution.api.instance:oiaaconta}")
    private String instanceNamePadrao;

    @Value("${evolution.webhook.url:http://oia-gateway:8080/webhook/evolution}")
    private String webhookUrl;

    @Value("${evolution.webhook.secret:}")
    private String webhookSecret;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() {};

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("apikey", apiKey);
        return h;
    }

    private String resolverInstancia(Long restauranteId) {
        if (restauranteId == null) return instanceNamePadrao;
        try {
            Map<String, String> info = authClient.getWhatsappInstance(restauranteId);
            String name = info != null ? info.get("instanceName") : null;
            return (name != null && !name.isBlank()) ? name : instanceNamePadrao;
        } catch (Exception e) {
            log.debug("Instância não encontrada para restaurante {}, usando padrão", restauranteId);
            return instanceNamePadrao;
        }
    }

    public WhatsappStatusDto status(Long restauranteId) {
        String instancia = resolverInstancia(restauranteId);
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                apiUrl + "/instance/connectionState/" + instancia,
                HttpMethod.GET, new HttpEntity<>(headers()), MAP_TYPE);
            String state = extractState(resp.getBody());
            if ("open".equals(state)) {
                return WhatsappStatusDto.builder().estado("CONECTADO").build();
            }
            if ("qrcode".equals(state) || "connecting".equals(state)) {
                try {
                    ResponseEntity<Map<String, Object>> qrResp = restTemplate.exchange(
                        apiUrl + "/instance/connect/" + instancia,
                        HttpMethod.GET, new HttpEntity<>(headers()), MAP_TYPE);
                    Map<String, Object> qrBody = qrResp.getBody();
                    return WhatsappStatusDto.builder()
                        .estado("AGUARDANDO_SCAN")
                        .qrCodeBase64(extractBase64(qrBody))
                        .qrCodeRaw(extractCode(qrBody))
                        .build();
                } catch (Exception qrEx) {
                    log.debug("Erro ao buscar QR code no status: {}", qrEx.getMessage());
                }
                return WhatsappStatusDto.builder().estado("AGUARDANDO_SCAN").build();
            }
        } catch (Exception e) {
            log.debug("Status check: {}", e.getMessage());
        }
        return WhatsappStatusDto.builder().estado("DESCONECTADO").build();
    }

    public WhatsappStatusDto conectar(Long restauranteId) {
        String instancia = resolverInstancia(restauranteId);
        try {
            garantirInstancia(instancia);

            // Vincula a instância ao restaurante para o chatbot funcionar
            if (restauranteId != null) {
                try {
                    authClient.salvarInstanciaWhatsapp(restauranteId, Map.of("instanceName", instancia));
                } catch (Exception e) {
                    log.warn("Erro ao salvar instância no auth-service: {}", e.getMessage());
                }
            }

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                apiUrl + "/instance/connect/" + instancia,
                HttpMethod.GET, new HttpEntity<>(headers()), MAP_TYPE);

            Map<String, Object> body = resp.getBody();
            String state = extractState(body);

            if ("open".equals(state)) {
                return WhatsappStatusDto.builder().estado("CONECTADO").build();
            }

            return WhatsappStatusDto.builder()
                .estado("AGUARDANDO_SCAN")
                .qrCodeBase64(extractBase64(body))
                .qrCodeRaw(extractCode(body))
                .build();
        } catch (Exception e) {
            log.error("Erro ao conectar Evolution API: {}", e.getMessage());
            return WhatsappStatusDto.builder().estado("ERRO").mensagem(e.getMessage()).build();
        }
    }

    public void enviarMensagemTexto(Long restauranteId, String telefone, String texto) {
        String instancia = resolverInstancia(restauranteId);
        Map<String, Object> body = Map.of("number", telefone, "text", texto);
        restTemplate.postForEntity(
            apiUrl + "/message/sendText/" + instancia,
            new HttpEntity<>(body, headers()),
            Void.class);
    }

    public void desconectar(Long restauranteId) {
        String instancia = resolverInstancia(restauranteId);
        try {
            restTemplate.exchange(
                apiUrl + "/instance/logout/" + instancia,
                HttpMethod.DELETE, new HttpEntity<>(headers()), MAP_TYPE);
        } catch (Exception e) {
            log.warn("Erro ao desconectar: {}", e.getMessage());
        }
    }

    private void garantirInstancia(String instancia) {
        try {
            restTemplate.exchange(
                apiUrl + "/instance/connectionState/" + instancia,
                HttpMethod.GET, new HttpEntity<>(headers()), MAP_TYPE);
            configurarWebhookSeExterno(instancia);
        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("instanceName", instancia);
            body.put("integration", "WHATSAPP-BAILEYS");
            body.put("qrcode", true);
            restTemplate.postForEntity(
                apiUrl + "/instance/create",
                new HttpEntity<>(body, headers()),
                Void.class);
            configurarWebhookSeExterno(instancia);
        }
    }

    private void configurarWebhookSeExterno(String instancia) {
        if (!isWebhookUrlExterno(webhookUrl)) {
            log.debug("Webhook URL local, pulando configuração: {}", webhookUrl);
            return;
        }
        try {
            Map<String, Object> webhookConfig = new HashMap<>();
            webhookConfig.put("url", webhookUrl);
            webhookConfig.put("enabled", true);
            webhookConfig.put("webhookByEvents", false);
            webhookConfig.put("events", List.of("MESSAGES_UPSERT"));
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                webhookConfig.put("webhookSecret", webhookSecret);
            }
            Map<String, Object> body = Map.of("webhook", webhookConfig);
            restTemplate.postForEntity(
                apiUrl + "/webhook/set/" + instancia,
                new HttpEntity<>(body, headers()),
                Void.class);
            log.info("Webhook configurado para instância {}: {}", instancia, webhookUrl);
        } catch (Exception e) {
            log.warn("Erro ao configurar webhook: {}", e.getMessage());
        }
    }

    private boolean isWebhookUrlExterno(String url) {
        if (url == null || url.isBlank()) return false;
        return !url.contains("localhost") && !url.contains("127.0.0.1");
    }

    private String extractState(Map<String, Object> body) {
        if (body == null) return "close";
        if (body.containsKey("instance") && body.get("instance") instanceof Map<?, ?> inst) {
            Object state = inst.get("state");
            return String.valueOf(state != null ? state : "close");
        }
        Object state = body.get("state");
        return String.valueOf(state != null ? state : "close");
    }

    private String extractBase64(Map<String, Object> body) {
        if (body == null) return null;
        if (body.containsKey("base64")) return nullIfNull(body.get("base64"));
        if (body.get("qrcode") instanceof Map<?, ?> qr) {
            if (qr.containsKey("base64")) return nullIfNull(qr.get("base64"));
        }
        return null;
    }

    private String extractCode(Map<String, Object> body) {
        if (body == null) return null;
        if (body.containsKey("code")) return nullIfNull(body.get("code"));
        if (body.get("qrcode") instanceof Map<?, ?> qr) {
            if (qr.containsKey("code")) return nullIfNull(qr.get("code"));
        }
        return null;
    }

    private String nullIfNull(Object val) {
        if (val == null || "null".equals(String.valueOf(val))) return null;
        return String.valueOf(val);
    }
}
