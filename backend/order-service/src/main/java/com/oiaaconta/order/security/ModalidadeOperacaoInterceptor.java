package com.oiaaconta.order.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.order.client.AuditoriaClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;

// Restringe /api/mesas/** e /api/comandas/** (fluxo de mesa/garçom) a
// restaurantes cuja modalidade não seja DELIVERY, e /api/entregas/**
// (fluxo de delivery/entregador) a restaurantes cuja modalidade não seja
// MESAS. Só planos como o Startup têm essa restrição (restringeModalidade=
// false nos demais, que passam direto).
@Component
@RequiredArgsConstructor
@Slf4j
public class ModalidadeOperacaoInterceptor implements HandlerInterceptor {

    private final AuditoriaClient billingClient;
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

        String path = request.getRequestURI();
        boolean rotaDeMesa = path.startsWith("/api/mesas") || path.startsWith("/api/comandas");
        boolean rotaDeEntrega = path.startsWith("/api/entregas");
        if (!rotaDeMesa && !rotaDeEntrega) return true;

        AuditoriaClient.RestricoesOperacaoResponse restricoes = buscarRestricoes(restauranteId);
        if (!restricoes.isRestringeModalidade()) return true;

        boolean bloqueado = (rotaDeMesa && "DELIVERY".equals(restricoes.getModalidadeOperacao()))
            || (rotaDeEntrega && "MESAS".equals(restricoes.getModalidadeOperacao()));
        if (bloqueado) {
            String mensagem = rotaDeMesa
                ? "Seu plano está configurado para delivery — o modo presencial (mesas e comandas) não está disponível."
                : "Seu plano está configurado para o modo presencial — delivery não está disponível.";
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status", 403,
                "message", mensagem,
                "timestamp", LocalDateTime.now().toString()
            )));
            return false;
        }
        return true;
    }

    // Indisponibilidade do billing-service não pode derrubar mesas/delivery
    // pra todo mundo — falha aberta (não restringe).
    private AuditoriaClient.RestricoesOperacaoResponse buscarRestricoes(Long restauranteId) {
        try {
            AuditoriaClient.RestricoesOperacaoResponse dto = billingClient.buscarRestricoesOperacao(restauranteId);
            if (dto != null) return dto;
        } catch (Exception e) {
            log.warn("Não foi possível consultar modalidade de operação do restaurante {}: {}", restauranteId, e.getMessage());
        }
        return new AuditoriaClient.RestricoesOperacaoResponse();
    }
}
