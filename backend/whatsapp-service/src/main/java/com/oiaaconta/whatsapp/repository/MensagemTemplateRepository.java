package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.MensagemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MensagemTemplateRepository extends JpaRepository<MensagemTemplate, Long> {
    Optional<MensagemTemplate> findByRestauranteIdAndChave(Long restauranteId, String chave);
    List<MensagemTemplate> findByRestauranteId(Long restauranteId);
}
