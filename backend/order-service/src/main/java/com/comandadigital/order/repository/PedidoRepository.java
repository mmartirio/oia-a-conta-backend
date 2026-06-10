package com.comandadigital.order.repository;

import com.comandadigital.order.entity.Pedido;
import com.comandadigital.order.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByRestauranteIdAndStatusInOrderByCreatedAtAsc(Long restauranteId, List<StatusPedido> statuses);
    Optional<Pedido> findByIdAndRestauranteId(Long id, Long restauranteId);
}
