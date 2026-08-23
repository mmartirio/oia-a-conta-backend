package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Promocao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromocaoRepository extends JpaRepository<Promocao, Long> {
    List<Promocao> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId);
    Optional<Promocao> findByIdAndRestauranteId(Long id, Long restauranteId);
    List<Promocao> findByRestauranteIdAndAtivoTrue(Long restauranteId);
}
