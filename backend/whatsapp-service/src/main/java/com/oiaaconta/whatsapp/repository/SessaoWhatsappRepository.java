package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessaoWhatsappRepository extends JpaRepository<SessaoWhatsapp, Long> {
    Optional<SessaoWhatsapp> findByTelefoneAndRestauranteId(String telefone, Long restauranteId);
    Optional<SessaoWhatsapp> findByEntregaId(Long entregaId);
}
