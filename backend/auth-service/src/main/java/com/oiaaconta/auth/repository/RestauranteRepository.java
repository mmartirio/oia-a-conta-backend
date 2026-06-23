package com.oiaaconta.auth.repository;

import com.oiaaconta.auth.entity.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    Optional<Restaurante> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByNomeIgnoreCase(String nome);
    Optional<Restaurante> findByWhatsappInstanceName(String whatsappInstanceName);
}
