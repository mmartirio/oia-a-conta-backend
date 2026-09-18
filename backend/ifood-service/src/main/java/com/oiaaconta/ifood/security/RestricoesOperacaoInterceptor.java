package com.oiaaconta.ifood.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.ifood.client.BillingClient;
import com.oiaaconta.ifood.dto.billing.RestricoesOperacaoDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;

// Restringe /api/ifood/admin/** a restaurantes cujo plano/modalidade
// permitem iFood — hoje, planos como o Startup só liberam iFood na
// modalidade delivery (presencial não integra). Planos sem restrição de
// modalidade sempre permitem.
@Component
@RequiredArgsConstructor
@Slf4j
public class RestricoesOperacaoInterceptor implements HandlerInterceptor {

    private final BillingClient billingClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String restauranteIdHeader = request.getHeader("X-Restaurante-Id");
        if (restauranteIdHeader == null) return true;

        Long restauranteId;
        try {
            restauranteId = Long.valueOf(restauranteIdHeader);
        } catch (NumberFormatException e) {
            return true;
        }

        if (!buscarRestricoes(restauranteId).isPermiteIfood()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status", 403,
                "message", "Seu plano está configurado para o modo presencial — a integração com o iFood não está disponível.",
                "timestamp", LocalDateTime.now().toString()
            )));
            return false;
        }
        return true;
    }

    // Indisponibilidade do billing-service não pode derrubar o iFood pra
    // todo mundo — falha aberta (permite).
    private RestricoesOperacaoDto buscarRestricoes(Long restauranteId) {
        try {
            RestricoesOperacaoDto dto = billingClient.buscarRestricoes(restauranteId);
            if (dto != null) return dto;
        } catch (Exception e) {
            log.warn("Não foi possível consultar restrições de operação do restaurante {}: {}", restauranteId, e.getMessage());
        }
        RestricoesOperacaoDto fallback = new RestricoesOperacaoDto();
        fallback.setPermiteIfood(true);
        return fallback;
    }
}
