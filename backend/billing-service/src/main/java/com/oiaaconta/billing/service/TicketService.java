package com.oiaaconta.billing.service;

import com.oiaaconta.billing.client.WhatsappInternalClient;
import com.oiaaconta.billing.entity.MensagemTicket;
import com.oiaaconta.billing.entity.TicketSuporte;
import com.oiaaconta.billing.enums.StatusTicket;
import com.oiaaconta.billing.repository.TicketSuporteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketSuporteRepository ticketRepository;
    private final WhatsappInternalClient whatsappInternalClient;

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

    // Chamado pelo whatsapp-service (via /internal/tickets/whatsapp) a cada
    // mensagem recebida no número de suporte da plataforma. Reaproveita um
    // ticket aberto do mesmo telefone se existir, em vez de abrir um novo por
    // mensagem.
    @Transactional
    @SuppressWarnings("null")
    public void registrarMensagemWhatsapp(String telefone, String nomeContato, String mensagem) {
        TicketSuporte ticket = ticketRepository
            .findFirstByWhatsappTelefoneAndStatusNotOrderByCreatedAtDesc(telefone, StatusTicket.FECHADO)
            .orElseGet(() -> ticketRepository.save(TicketSuporte.builder()
                .whatsappTelefone(telefone)
                .whatsappNome(nomeContato)
                .origem("WHATSAPP")
                .titulo("Chamado via WhatsApp" + (nomeContato != null && !nomeContato.isBlank() ? " — " + nomeContato : ""))
                .descricao(mensagem)
                .build()));
        adicionarMensagem(ticket.getId(), null, nomeContato, "CLIENTE", mensagem);
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

        // Ticket originado por WhatsApp + resposta do suporte → devolve a
        // mensagem pro mesmo contato, fechando o loop pelo mesmo canal.
        if ("SUPORTE".equals(remetenteTipo) && ticket.getWhatsappTelefone() != null) {
            try {
                whatsappInternalClient.enviarSuporte(Map.of(
                    "telefone", ticket.getWhatsappTelefone(),
                    "mensagem", mensagem));
            } catch (Exception e) {
                log.warn("Falha ao enviar resposta do ticket {} via WhatsApp: {}", ticketId, e.getMessage());
            }
        }

        return msg;
    }
}
