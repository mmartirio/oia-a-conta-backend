package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.enums.StatusContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    @EntityGraph(attributePaths = "plano")
    Optional<Contrato> findByRestauranteId(Long restauranteId);

    // Sem isso, "plano" fica como proxy lazy não inicializado e quebra a
    // serialização Jackson da lista (ver jackson-datatype-hibernate6 no pom).
    @Override
    @NonNull
    @EntityGraph(attributePaths = "plano")
    Page<Contrato> findAll(@NonNull Pageable pageable);

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
