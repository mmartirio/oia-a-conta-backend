package com.oiaaconta.billing.service;

import com.oiaaconta.billing.entity.MensagemTicket;
import com.oiaaconta.billing.entity.TicketSuporte;
import com.oiaaconta.billing.enums.StatusTicket;
import com.oiaaconta.billing.repository.TicketSuporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketSuporteRepository ticketRepository;

    public Page<TicketSuporte> listarTodos(Pageable pageable) {
        return ticketRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<TicketSuporte> listarPorRestaurante(Long restauranteId) {
        return ticketRepository.findByRestauranteIdOrderByCreatedAtDesc(restauranteId);
    }

    public TicketSuporte buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id é obrigatório");
        }
        return ticketRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Ticket não encontrado"));
    }

    @Transactional
    @SuppressWarnings("null")
    public TicketSuporte criar(TicketSuporte ticket) {
        return ticketRepository.save(ticket);
    }

    @Transactional
    @SuppressWarnings("null")
    public TicketSuporte atualizarStatus(Long id, StatusTicket novoStatus) {
        TicketSuporte ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Ticket não encontrado"));
        ticket.setStatus(novoStatus);
        if (novoStatus == StatusTicket.RESOLVIDO || novoStatus == StatusTicket.FECHADO) {
            ticket.setResolvidoAt(LocalDateTime.now());
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    @SuppressWarnings("null")
    public MensagemTicket adicionarMensagem(Long ticketId, Long remetenteId, String remetenteNome,
                                             String remetenteTipo, String mensagem) {
        TicketSuporte ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new NoSuchElementException("Ticket não encontrado"));
        MensagemTicket msg = MensagemTicket.builder()
            .ticket(ticket)
            .remetenteId(remetenteId)
            .remetenteNome(remetenteNome)
            .remetenteTipo(remetenteTipo)
            .mensagem(mensagem)
            .build();
        ticket.getMensagens().add(msg);
        ticketRepository.save(ticket);
        return msg;
    }
}
