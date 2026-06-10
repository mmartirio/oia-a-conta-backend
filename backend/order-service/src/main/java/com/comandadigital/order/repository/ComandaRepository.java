package com.comandadigital.order.repository;

import com.comandadigital.order.entity.Comanda;
import com.comandadigital.order.enums.StatusComanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    Optional<Comanda> findByMesaIdAndStatusAndRestauranteId(Long mesaId, StatusComanda status, Long restauranteId);
    List<Comanda> findByRestauranteIdAndStatus(Long restauranteId, StatusComanda status);
    Optional<Comanda> findByIdAndRestauranteId(Long id, Long restauranteId);
}
