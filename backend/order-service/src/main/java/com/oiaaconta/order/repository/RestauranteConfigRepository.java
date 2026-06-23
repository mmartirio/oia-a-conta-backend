package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.RestauranteConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestauranteConfigRepository extends JpaRepository<RestauranteConfig, Long> {
    Optional<RestauranteConfig> findByRestauranteId(Long restauranteId);
}
