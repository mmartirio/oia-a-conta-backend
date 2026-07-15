package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.TicketSuporte;
import com.oiaaconta.billing.enums.StatusTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSuporteRepository extends JpaRepository<TicketSuporte, Long> {
    List<TicketSuporte> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId);
    List<TicketSuporte> findByStatusOrderByCreatedAtDesc(StatusTicket status);
    Page<TicketSuporte> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(StatusTicket status);
}
