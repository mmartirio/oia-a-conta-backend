package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    Page<MovimentacaoEstoque> findByProdutoIdAndRestauranteIdOrderByCriadoEmDesc(Long produtoId, Long restauranteId, Pageable pageable);
}
