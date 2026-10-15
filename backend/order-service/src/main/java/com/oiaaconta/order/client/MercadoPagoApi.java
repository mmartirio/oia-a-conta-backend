package com.oiaaconta.order.client;

import com.oiaaconta.order.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

// Chamadas REST à API do Mercado Pago com o access token de quem for o
// dono do recurso (o restaurante, no pagamento online). Erros da API viram
// BusinessException com a mensagem do MP — chega ao frontend como 400.
@Component
@Slf4j
public class MercadoPagoApi {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    private final RestClient client = RestClient.builder().baseUrl("https://api.mercadopago.com").build();

    public Map<String, Object> get(String accessToken, String path) {
        try {
            Map<String, Object> r = client.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve().body(JSON);
            return r != null ? r : Map.of();
        } catch (RestClientResponseException e) {
            throw erro("GET", path, e);
        }
    }

    // X-Idempotency-Key é obrigatório em /v1/payments — uma chave nova por
    // tentativa (o retry de verdade é o cliente clicar de novo).
    public Map<String, Object> post(String accessToken, String path, Object body) {
        try {
            var req = client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", UUID.randomUUID().toString());
            if (accessToken != null) req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            Map<String, Object> r = req.body(body).retrieve().body(JSON);
            return r != null ? r : Map.of();
        } catch (RestClientResponseException e) {
            throw erro("POST", path, e);
        }
    }

    private BusinessException erro(String metodo, String path, RestClientResponseException e) {
        log.error("Mercado Pago {} {} → {}: {}", metodo, path.replaceAll("\\?.*", ""), e.getStatusCode().value(), e.getResponseBodyAsString());
        Map<String, Object> corpo = null;
        try { corpo = e.getResponseBodyAs(JSON); } catch (Exception ignored) { }
        Object msg = corpo != null ? corpo.get("message") : null;
        return new BusinessException("Mercado Pago recusou a operação" + (msg != null ? ": " + msg : "."));
    }
}
