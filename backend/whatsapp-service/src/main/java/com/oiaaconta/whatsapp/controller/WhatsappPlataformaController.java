package com.oiaaconta.whatsapp.controller;

import com.oiaaconta.whatsapp.dto.WhatsappStatusDto;
import com.oiaaconta.whatsapp.service.WhatsappAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// WhatsApp único da plataforma (não vinculado a um restaurante) — usado pelo
// suporte pra receber chamados dos administradores dos restaurantes, que
// viram tickets em /gestor/tickets (ver WebhookController e TicketService no
// billing-service).
@RestController
@RequestMapping("/api/whatsapp/plataforma")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class WhatsappPlataformaController {

    private final WhatsappAdminService adminService;

    @GetMapping("/status")
    public ResponseEntity<WhatsappStatusDto> status() {
        return ResponseEntity.ok(adminService.status(null));
    }

    @PostMapping("/conectar")
    public ResponseEntity<WhatsappStatusDto> conectar() {
        return ResponseEntity.ok(adminService.conectar(null));
    }

    @DeleteMapping("/desconectar")
    public ResponseEntity<Void> desconectar() {
        adminService.desconectar(null);
        return ResponseEntity.ok().build();
    }
}
