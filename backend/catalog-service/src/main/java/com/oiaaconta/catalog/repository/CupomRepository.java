package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Cupom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CupomRepository extends JpaRepository<Cupom, Long> {
    List<Cupom> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId);
    Optional<Cupom> findByIdAndRestauranteId(Long id, Long restauranteId);
    Optional<Cupom> findByRestauranteIdAndCodigo(Long restauranteId, String codigo);
    boolean existsByRestauranteIdAndCodigo(Long restauranteId, String codigo);
}
