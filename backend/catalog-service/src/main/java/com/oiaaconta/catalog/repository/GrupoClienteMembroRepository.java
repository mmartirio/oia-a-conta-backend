package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.GrupoClienteMembro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoClienteMembroRepository extends JpaRepository<GrupoClienteMembro, Long> {
    List<GrupoClienteMembro> findByGrupoClienteIdAndRestauranteId(Long grupoClienteId, Long restauranteId);
    long countByGrupoClienteId(Long grupoClienteId);
    Optional<GrupoClienteMembro> findByGrupoClienteIdAndClienteIdAndRestauranteId(
        Long grupoClienteId, Long clienteId, Long restauranteId);
    boolean existsByGrupoClienteIdAndClienteId(Long grupoClienteId, Long clienteId);
    List<GrupoClienteMembro> findByClienteIdAndRestauranteId(Long clienteId, Long restauranteId);
}
