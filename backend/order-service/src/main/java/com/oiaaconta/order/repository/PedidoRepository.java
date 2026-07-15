package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusPedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    // itens agora é FetchType.LAZY (era EAGER) — estes dois finders são usados
    // em fluxos de leitura sem transação própria (PedidoService.listarAtivos,
    // FinanceiroService.getComissoes), então usam @EntityGraph para carregar
    // "itens" numa única query em vez de depender de lazy-loading fora de tx.
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByRestauranteIdAndStatusInOrderByCreatedAtAsc(Long restauranteId, List<StatusPedido> statuses);

    Optional<Pedido> findByIdAndRestauranteId(Long id, Long restauranteId);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByRestauranteIdAndCozinheiroIdNotNullAndCreatedAtBetween(
        Long restauranteId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    long countByRestauranteIdAndStatusInAndCreatedAtBetween(
        Long restauranteId, List<StatusPedido> status, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    long countByRestauranteIdAndCreatedAtBetween(
        Long restauranteId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}
