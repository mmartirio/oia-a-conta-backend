package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.enums.StatusEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    List<Entrega> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId);
    List<Entrega> findByRestauranteIdAndStatusOrderByCreatedAtDesc(Long restauranteId, StatusEntrega status);
    Optional<Entrega> findByIdAndRestauranteId(Long id, Long restauranteId);
    List<Entrega> findByRestauranteIdAndStatusAndPagamentoConfirmadoCaixaOrderByCreatedAtDesc(
        Long restauranteId, StatusEntrega status, Boolean pagamentoConfirmadoCaixa);
    List<Entrega> findByRestauranteIdAndPagamentoConfirmadoCaixaTrueAndEntregueAtBetween(
        Long restauranteId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}
