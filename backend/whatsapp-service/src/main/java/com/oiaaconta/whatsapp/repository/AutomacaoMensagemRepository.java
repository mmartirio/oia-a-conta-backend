package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.AutomacaoMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutomacaoMensagemRepository extends JpaRepository<AutomacaoMensagem, Long> {
    List<AutomacaoMensagem> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId);
    List<AutomacaoMensagem> findByRestauranteIdAndAtivoTrue(Long restauranteId);
    Optional<AutomacaoMensagem> findByIdAndRestauranteId(Long id, Long restauranteId);
}
