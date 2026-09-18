package com.oiaaconta.table.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.table.client.BillingClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;

// Restringe /api/mesas/** a restaurantes cuja modalidade de operação (só
// existe em planos como o Startup) não seja DELIVERY. Restaurantes em
// planos sem essa restrição (restringeModalidade=false) passam direto. O
// limite de quantidade de mesas por plano já é tratado à parte, em
// MesaService.verificarLimiteMesas — isto aqui é sobre disponibilidade do
// recurso "mesas" por modalidade, não sobre contagem.
@Component
@RequiredArgsConstructor
@Slf4j
public class ModalidadeOperacaoInterceptor implements HandlerInterceptor {

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

        BillingClient.RestricoesOperacaoResponse restricoes = buscarRestricoes(restauranteId);
        if (restricoes.isRestringeModalidade() && "DELIVERY".equals(restricoes.getModalidadeOperacao())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status", 403,
                "message", "Seu plano está configurado para delivery — o modo presencial (mesas) não está disponível.",
                "timestamp", LocalDateTime.now().toString()
            )));
            return false;
        }
        return true;
    }

    // Indisponibilidade do billing-service não pode derrubar mesas pra todo
    // mundo — falha aberta (não restringe).
    private BillingClient.RestricoesOperacaoResponse buscarRestricoes(Long restauranteId) {
        try {
            BillingClient.RestricoesOperacaoResponse dto = billingClient.buscarRestricoesOperacao(restauranteId);
            if (dto != null) return dto;
        } catch (Exception e) {
            log.warn("Não foi possível consultar modalidade de operação do restaurante {}: {}", restauranteId, e.getMessage());
        }
        return new BillingClient.RestricoesOperacaoResponse();
    }
}
