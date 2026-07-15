package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.WhatsappConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsappConfigRepository extends JpaRepository<WhatsappConfig, Long> {
    Optional<WhatsappConfig> findByRestauranteId(Long restauranteId);
}
