package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.entity.MensagemTicket;
import com.oiaaconta.billing.entity.TicketSuporte;
import com.oiaaconta.billing.enums.PrioridadeTicket;
import com.oiaaconta.billing.enums.StatusTicket;
import com.oiaaconta.billing.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<TicketSuporte>> listarTodos(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ticketService.listarTodos(pageable));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<TicketSuporte>> listarMeus(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(ticketService.listarPorRestaurante(restauranteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TicketSuporte> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TicketSuporte> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Nome", required = false) String userNome,
            @RequestBody Map<String, String> body) {
        TicketSuporte ticket = TicketSuporte.builder()
            .restauranteId(restauranteId)
            .usuarioId(userId)
            .titulo(body.get("titulo"))
            .descricao(body.get("descricao"))
            .prioridade(body.containsKey("prioridade")
                ? PrioridadeTicket.valueOf(body.get("prioridade")) : PrioridadeTicket.MEDIA)
            .build();
        return ResponseEntity.status(201).body(ticketService.criar(ticket));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TicketSuporte> atualizarStatus(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ticketService.atualizarStatus(id, StatusTicket.valueOf(body.get("status"))));
    }

    @PostMapping("/{id}/mensagens")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MensagemTicket> adicionarMensagem(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Nome", required = false) String userNome,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestBody Map<String, String> body) {
        String tipo = "SUPER_ADMIN".equals(userRole) ? "SUPORTE" : "CLIENTE";
        return ResponseEntity.status(201).body(
            ticketService.adicionarMensagem(id, userId, userNome, tipo, body.get("mensagem")));
    }
}
