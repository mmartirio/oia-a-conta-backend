package com.oiaaconta.whatsapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.whatsapp.client.BillingClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;

// Bloqueia /api/whatsapp/admin/** por inteiro quando o plano/modalidade do
// restaurante zera o limite de atendentes de WhatsApp (ex: plano Startup na
// modalidade presencial). Quando o limite é > 0 (ex: Startup delivery = 1,
// Completo = 3), a contagem já é validada na concessão da permissão — ver
// AtendenteWhatsappService no auth-service — aqui só cobre o caso "zero".
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

        if (limiteZerado(restauranteId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status", 403,
                "message", "Seu plano não inclui atendimento por WhatsApp.",
                "timestamp", LocalDateTime.now().toString()
            )));
            return false;
        }
        return true;
    }

    // Indisponibilidade do billing-service não pode derrubar o WhatsApp pra
    // todo mundo — falha aberta (não bloqueia).
    private boolean limiteZerado(Long restauranteId) {
        try {
            BillingClient.RestricoesOperacaoDto dto = billingClient.buscarRestricoes(restauranteId);
            Integer limite = dto != null ? dto.getLimiteAtendentesWhatsapp() : null;
            return limite != null && limite == 0;
        } catch (Exception e) {
            log.warn("Não foi possível consultar limite de atendentes de WhatsApp do restaurante {}: {}", restauranteId, e.getMessage());
            return false;
        }
    }
}
