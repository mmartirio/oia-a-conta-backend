package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, Long> {
    Optional<MetaFinanceira> findByRestauranteIdAndMes(Long restauranteId, String mes);
}
