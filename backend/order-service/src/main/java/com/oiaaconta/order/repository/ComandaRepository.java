package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.enums.StatusComanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    Optional<Comanda> findByMesaIdAndStatusAndRestauranteId(Long mesaId, StatusComanda status, Long restauranteId);
    List<Comanda> findByRestauranteIdAndStatus(Long restauranteId, StatusComanda status);
    Optional<Comanda> findByIdAndRestauranteId(Long id, Long restauranteId);
    List<Comanda> findByRestauranteIdAndStatusAndClosedAtBetween(
        Long restauranteId, StatusComanda status,
        java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}
