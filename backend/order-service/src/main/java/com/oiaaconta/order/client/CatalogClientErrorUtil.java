package com.oiaaconta.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;

// Extrai a mensagem de negócio do corpo de erro do catalog-service
// ({"status":400,"message":"...","timestamp":"..."}, formato do
// GlobalExceptionHandler dele) pra repassar como BusinessException legível
// no order-service, em vez de vazar o corpo bruto do Feign.
public final class CatalogClientErrorUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CatalogClientErrorUtil() { }

    public static String extrairMensagem(FeignException e, String fallback) {
        String corpo = e.contentUTF8();
        if (corpo == null || corpo.isBlank()) return fallback;
        try {
            var node = MAPPER.readTree(corpo);
            String mensagem = node.hasNonNull("message") ? node.get("message").asText() : null;
            return mensagem != null && !mensagem.isBlank() ? mensagem : fallback;
        } catch (Exception parseError) {
            return fallback;
        }
    }
}
