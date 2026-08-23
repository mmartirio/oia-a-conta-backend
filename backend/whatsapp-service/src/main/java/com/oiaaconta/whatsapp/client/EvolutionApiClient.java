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

    // Evolution API aceita a imagem em base64 puro (sem o prefixo "data:...")
    // ou uma URL — mandamos o base64 puro já que a imagem vem do upload do
    // admin (data URI), só cortando o prefixo antes de enviar.
    public void enviarImagem(String telefone, String imagemBase64OuDataUri, String legenda) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            String media = imagemBase64OuDataUri.contains(",")
                ? imagemBase64OuDataUri.substring(imagemBase64OuDataUri.indexOf(',') + 1)
                : imagemBase64OuDataUri;

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("number", telefone);
            body.put("mediatype", "image");
            body.put("media", media);
            body.put("caption", legenda == null ? "" : legenda);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                apiUrl + "/message/sendMedia/" + instanceName,
                entity, Object.class);
        } catch (Exception e) {
            log.error("Erro ao enviar imagem WhatsApp para {}: {}", telefone, e.getMessage());
        }
    }

    public void enviarBotaoResposta(String telefone, String titulo, String descricao, String textoBotao, String id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            Map<String, Object> botao = Map.of(
                "type", "reply",
                "displayText", textoBotao,
                "id", id
            );

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("number", telefone);
            body.put("title", titulo);
            body.put("description", descricao);
            body.put("footer", "");
            body.put("buttons", java.util.List.of(botao));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                apiUrl + "/message/sendButtons/" + instanceName,
                entity, Object.class);
        } catch (Exception e) {
            log.error("Erro ao enviar botao resposta para {}: {}", telefone, e.getMessage());
        }
    }

    public void enviarBotaoLink(String telefone, String titulo, String descricao, String textoBotao, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            Map<String, Object> botao = Map.of(
                "type", "url",
                "displayText", textoBotao,
                "url", url
            );

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("number", telefone);
            body.put("title", titulo);
            body.put("description", descricao);
            body.put("footer", "");
            body.put("buttons", java.util.List.of(botao));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                apiUrl + "/message/sendButtons/" + instanceName,
                entity, Object.class);
        } catch (Exception e) {
            log.error("Erro ao enviar botão WhatsApp para {}: {}", telefone, e.getMessage());
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
                private ButtonsResponsePayload buttonsResponseMessage;
                private ButtonsResponsePayload templateButtonReplyMessage;

                @Data
                public static class ExtendedTextPayload {
                    private String text;
                }

                @Data
                public static class ButtonsResponsePayload {
                    private String selectedButtonId;
                    private String selectedDisplayText;
                }

                public String getText() {
                    if (conversation != null) return conversation;
                    if (extendedTextMessage != null) return extendedTextMessage.getText();
                    if (buttonsResponseMessage != null) return buttonsResponseMessage.getSelectedButtonId();
                    if (templateButtonReplyMessage != null) return templateButtonReplyMessage.getSelectedButtonId();
                    return null;
                }
            }
        }
    }
}
