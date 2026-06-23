package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByRestauranteIdAndAtivoTrueOrderByNomeAsc(Long restauranteId);
    Optional<Categoria> findByIdAndRestauranteId(Long id, Long restauranteId);
    boolean existsByRestauranteIdAndNome(Long restauranteId, String nome);
}
