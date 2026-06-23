package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.entity.Plano;
import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final BillingService billingService;

    @GetMapping
    public ResponseEntity<List<Plano>> listar() {
        return ResponseEntity.ok(billingService.listarPlanosAtivos());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Plano>> listarTodos() {
        return ResponseEntity.ok(billingService.listarTodosPlanos());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Plano> criar(@RequestBody Plano plano) {
        return ResponseEntity.status(201).body(billingService.criarPlano(plano));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Plano> atualizar(@PathVariable Long id, @RequestBody Plano plano) {
        return ResponseEntity.ok(billingService.atualizarPlano(id, plano));
    }
}
