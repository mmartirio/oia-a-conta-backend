package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByRestauranteIdAndAtivoTrueOrderByOrdemAscNomeAsc(Long restauranteId);
    List<Categoria> findByRestauranteIdOrderByOrdemAscNomeAsc(Long restauranteId);
    Optional<Categoria> findByIdAndRestauranteId(Long id, Long restauranteId);
    boolean existsByRestauranteIdAndNome(Long restauranteId, String nome);
    List<Categoria> findByRestauranteIdAndIdIn(Long restauranteId, List<Long> ids);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(max(c.ordem), -1) from Categoria c where c.restauranteId = :restauranteId")
    int maiorOrdem(Long restauranteId);
}
