package com.oiaaconta.billing.service;

import com.oiaaconta.billing.dto.request.RegistrarLogRequest;
import com.oiaaconta.billing.dto.response.LogAuditoriaResponse;
import com.oiaaconta.billing.entity.LogAuditoria;
import com.oiaaconta.billing.repository.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogAuditoriaService {

    private final LogAuditoriaRepository repository;

    @Transactional
    public void registrar(RegistrarLogRequest request) {
        repository.save(LogAuditoria.builder()
            .restauranteId(request.getRestauranteId())
            .tipo(request.getTipo())
            .descricao(request.getDescricao())
            .usuarioId(request.getUsuarioId())
            .usuarioNome(request.getUsuarioNome())
            .build());
    }

    public Page<LogAuditoriaResponse> listar(Long restauranteId, String tipo, Pageable pageable) {
        Page<LogAuditoria> page = (tipo == null || tipo.isBlank())
            ? repository.findByRestauranteIdOrderByCriadoEmDesc(restauranteId, pageable)
            : repository.findByRestauranteIdAndTipoOrderByCriadoEmDesc(restauranteId, tipo, pageable);
        return page.map(this::toResponse);
    }

    private LogAuditoriaResponse toResponse(LogAuditoria l) {
        return LogAuditoriaResponse.builder()
            .id(l.getId())
            .restauranteId(l.getRestauranteId())
            .tipo(l.getTipo())
            .descricao(l.getDescricao())
            .usuarioId(l.getUsuarioId())
            .usuarioNome(l.getUsuarioNome())
            .criadoEm(l.getCriadoEm())
            .build();
    }
}
