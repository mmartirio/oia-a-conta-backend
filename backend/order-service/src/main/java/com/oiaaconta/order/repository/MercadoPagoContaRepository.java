package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.MercadoPagoConta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MercadoPagoContaRepository extends JpaRepository<MercadoPagoConta, Long> {
    Optional<MercadoPagoConta> findByRestauranteId(Long restauranteId);

    List<MercadoPagoConta> findByRefreshTokenCifradoNotNullAndExpiraEmBefore(LocalDateTime limite);
}
