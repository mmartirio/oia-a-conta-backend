package com.oiaaconta.whatsapp.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvolutionApiClient {

    private final RestTemplate restTemplate;

    @Value("${evolution.api.url}")
    private String apiUrl;

    @Value("${evolution.api.key}")
    private String apiKey;

    @Value("${evolution.api.instance}")
    private String instanceName;

    public void enviarMensagem(String telefone, String texto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            Map<String, Object> body = Map.of(
                "number", telefone,
                "text", texto
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                apiUrl + "/message/sendText/" + instanceName,
                entity, Object.class);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem WhatsApp para {}: {}", telefone, e.getMessage());
        }
    }

    @Data
    public static class WebhookPayload {
        private String event;
        @JsonProperty("instance")
        private String instance;
        private DataPayload data;

        @Data
        public static class DataPayload {
            private String key_remoteJid;
            private MessagePayload message;
            private String pushName;

            @JsonProperty("key")
            private KeyPayload key;

            @Data
            public static class KeyPayload {
                private String remoteJid;
                private boolean fromMe;
                private String id;
            }

            @Data
            public static class MessagePayload {
                private String conversation;
                private ExtendedTextPayload extendedTextMessage;

                @Data
                public static class ExtendedTextPayload {
                    private String text;
                }

                public String getText() {
                    if (conversation != null) return conversation;
                    if (extendedTextMessage != null) return extendedTextMessage.getText();
                    return null;
                }
            }
        }
    }
}
