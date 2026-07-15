package com.oiaaconta.auth.repository;

import com.oiaaconta.auth.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByRestauranteIdOrderByNomeAsc(Long restauranteId);
    Optional<Grupo> findByIdAndRestauranteId(Long id, Long restauranteId);
}
