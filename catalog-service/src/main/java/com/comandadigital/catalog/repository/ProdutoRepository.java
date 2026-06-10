package com.comandadigital.catalog.repository;

import com.comandadigital.catalog.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByRestauranteIdAndAtivoTrueOrderByNomeAsc(Long restauranteId);
    List<Produto> findByCategoriaIdAndRestauranteIdAndAtivoTrue(Long categoriaId, Long restauranteId);
    Optional<Produto> findByIdAndRestauranteId(Long id, Long restauranteId);
}
