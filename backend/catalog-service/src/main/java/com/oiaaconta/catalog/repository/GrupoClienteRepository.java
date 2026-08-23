package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.GrupoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoClienteRepository extends JpaRepository<GrupoCliente, Long> {
    List<GrupoCliente> findByRestauranteIdOrderByNomeAsc(Long restauranteId);
    List<GrupoCliente> findByRestauranteIdAndAtivoTrueOrderByNomeAsc(Long restauranteId);
    Optional<GrupoCliente> findByIdAndRestauranteId(Long id, Long restauranteId);
}
