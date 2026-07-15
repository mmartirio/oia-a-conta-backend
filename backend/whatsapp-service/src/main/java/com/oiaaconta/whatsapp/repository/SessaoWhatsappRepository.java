package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import com.oiaaconta.whatsapp.enums.EstadoSessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessaoWhatsappRepository extends JpaRepository<SessaoWhatsapp, Long> {
    Optional<SessaoWhatsapp> findByTelefoneAndRestauranteId(String telefone, Long restauranteId);
    Optional<SessaoWhatsapp> findByEntregaId(Long entregaId);
    List<SessaoWhatsapp> findByRestauranteIdAndEstado(Long restauranteId, EstadoSessao estado);
}
