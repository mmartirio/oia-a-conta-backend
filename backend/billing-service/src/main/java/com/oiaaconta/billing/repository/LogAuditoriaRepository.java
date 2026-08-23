package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findByRestauranteIdOrderByCriadoEmDesc(Long restauranteId, Pageable pageable);

    Page<LogAuditoria> findByRestauranteIdAndTipoOrderByCriadoEmDesc(Long restauranteId, String tipo, Pageable pageable);
}
