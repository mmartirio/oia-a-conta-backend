package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findByRestauranteIdAndDataBetweenOrderByDataDesc(Long restauranteId, LocalDate inicio, LocalDate fim);

    Optional<Despesa> findByIdAndRestauranteId(Long id, Long restauranteId);

    @Query("SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d WHERE d.restauranteId = ?1 AND d.data BETWEEN ?2 AND ?3")
    BigDecimal somarPorPeriodo(Long restauranteId, LocalDate inicio, LocalDate fim);
}
