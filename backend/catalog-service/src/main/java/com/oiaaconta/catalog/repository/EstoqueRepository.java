package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    List<Estoque> findByRestauranteId(Long restauranteId);
    Optional<Estoque> findByProdutoIdAndRestauranteId(Long produtoId, Long restauranteId);
    List<Estoque> findByProdutoIdInAndRestauranteId(List<Long> produtoIds, Long restauranteId);
    List<Estoque> findByRestauranteIdAndControladoTrue(Long restauranteId);

    // Decremento atômico com checagem de saldo na mesma instrução — evita
    // corrida entre "ler saldo" e "gravar novo saldo" sob concorrência.
    // rowsAffected == 0 significa saldo insuficiente (ou produto_id inexistente).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Estoque e SET e.quantidade = e.quantidade - :qtd WHERE e.produtoId = :produtoId " +
        "AND e.restauranteId = :restauranteId AND e.quantidade >= :qtd")
    int baixarSeHouverSaldo(@Param("produtoId") Long produtoId, @Param("restauranteId") Long restauranteId, @Param("qtd") int qtd);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Estoque e SET e.quantidade = e.quantidade + :qtd WHERE e.produtoId = :produtoId " +
        "AND e.restauranteId = :restauranteId")
    int devolver(@Param("produtoId") Long produtoId, @Param("restauranteId") Long restauranteId, @Param("qtd") int qtd);
}
