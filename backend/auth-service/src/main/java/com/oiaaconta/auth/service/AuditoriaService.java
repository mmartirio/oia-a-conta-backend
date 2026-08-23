package com.oiaaconta.auth.service;

import com.oiaaconta.auth.client.AuditoriaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// Wrapper fino sobre o AuditoriaClient (billing-service) — nunca deve derrubar
// o fluxo de negócio principal (login, criação de usuário) se o registro de
// auditoria falhar, por isso roda assíncrono e engole qualquer erro.
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final AuditoriaClient auditoriaClient;

    @Async
    public void registrar(Long restauranteId, String tipo, String descricao, Long usuarioId, String usuarioNome) {
        if (restauranteId == null) return;
        try {
            auditoriaClient.registrar(AuditoriaClient.RegistrarLogRequest.builder()
                .restauranteId(restauranteId)
                .tipo(tipo)
                .descricao(descricao)
                .usuarioId(usuarioId)
                .usuarioNome(usuarioNome)
                .build());
        } catch (Exception e) {
            log.warn("Falha ao registrar log de auditoria ({}): {}", tipo, e.getMessage());
        }
    }
}
