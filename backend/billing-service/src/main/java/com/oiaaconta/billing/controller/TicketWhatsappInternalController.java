package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.dto.request.RegistrarMensagemWhatsappRequest;
import com.oiaaconta.billing.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoint interno (não roteado pelo api-gateway) — chamado pelo
// whatsapp-service a cada mensagem recebida no número de suporte da
// plataforma, pra criar/atualizar um ticket em nome do contato.
@RestController
@RequestMapping("/internal/tickets/whatsapp")
@RequiredArgsConstructor
public class TicketWhatsappInternalController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistrarMensagemWhatsappRequest request) {
        ticketService.registrarMensagemWhatsapp(request.getTelefone(), request.getNomeContato(), request.getMensagem());
        return ResponseEntity.ok().build();
    }
}
