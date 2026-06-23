package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Pedido;
import com.oiaaconta.order.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByRestauranteIdAndStatusInOrderByCreatedAtAsc(Long restauranteId, List<StatusPedido> statuses);
    Optional<Pedido> findByIdAndRestauranteId(Long id, Long restauranteId);
    List<Pedido> findByRestauranteIdAndCozinheiroIdNotNullAndCreatedAtBetween(
        Long restauranteId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}
