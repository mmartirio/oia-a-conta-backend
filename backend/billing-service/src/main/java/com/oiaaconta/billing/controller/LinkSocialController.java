package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.entity.LinkSocial;
import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/links-sociais")
@RequiredArgsConstructor
public class LinkSocialController {

    private final BillingService billingService;

    @GetMapping
    public ResponseEntity<List<LinkSocial>> listar() {
        return ResponseEntity.ok(billingService.listarLinksSociaisAtivos());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<LinkSocial>> listarTodos() {
        return ResponseEntity.ok(billingService.listarTodosLinksSociais());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<LinkSocial> criar(@RequestBody LinkSocial link) {
        return ResponseEntity.status(201).body(billingService.criarLinkSocial(link));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<LinkSocial> atualizar(@PathVariable Long id, @RequestBody LinkSocial link) {
        return ResponseEntity.ok(billingService.atualizarLinkSocial(id, link));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        billingService.deletarLinkSocial(id);
        return ResponseEntity.noContent().build();
    }
}
