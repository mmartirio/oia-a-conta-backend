package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.TicketSuporte;
import com.oiaaconta.billing.enums.StatusTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSuporteRepository extends JpaRepository<TicketSuporte, Long> {
    List<TicketSuporte> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId);
    List<TicketSuporte> findByStatusOrderByCreatedAtDesc(StatusTicket status);
    List<TicketSuporte> findAllByOrderByCreatedAtDesc();
    long countByStatus(StatusTicket status);
}
