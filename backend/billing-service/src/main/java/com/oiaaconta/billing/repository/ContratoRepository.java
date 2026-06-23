package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.enums.StatusContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Optional<Contrato> findByRestauranteId(Long restauranteId);

    @Query("""
        SELECT c FROM Contrato c
        WHERE c.status IN ('ATIVO','TRIAL','INADIMPLENTE')
          AND c.dataVencimento < :limite
          AND c.status != 'BLOQUEADO'
        """)
    List<Contrato> findVencidosAntesde(LocalDate limite);

    @Query("""
        SELECT c FROM Contrato c
        WHERE c.status IN ('ATIVO','INADIMPLENTE')
          AND c.dataVencimento < :hoje
          AND c.status != 'BLOQUEADO'
        """)
    List<Contrato> findInadimplentes(LocalDate hoje);

    List<Contrato> findByStatusOrderByCreatedAtDesc(StatusContrato status);
}
