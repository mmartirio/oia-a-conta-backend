package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByRestauranteIdOrderByNomeAsc(Long restauranteId);
    List<Combo> findByRestauranteIdAndAtivoTrueOrderByNomeAsc(Long restauranteId);
    Optional<Combo> findByIdAndRestauranteId(Long id, Long restauranteId);
    List<Combo> findByRestauranteIdAndAtivoTrueAndNumeroCardapioIsNotNullOrderByNumeroCardapioAsc(Long restauranteId);
}
