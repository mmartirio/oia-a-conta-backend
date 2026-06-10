package com.comandadigital.table.repository;

import com.comandadigital.table.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByRestauranteIdOrderByNumeroAsc(Long restauranteId);
    Optional<Mesa> findByIdAndRestauranteId(Long id, Long restauranteId);
    boolean existsByRestauranteIdAndNumero(Long restauranteId, Integer numero);
    Optional<Mesa> findByRestauranteIdAndNumero(Long restauranteId, Integer numero);
}
