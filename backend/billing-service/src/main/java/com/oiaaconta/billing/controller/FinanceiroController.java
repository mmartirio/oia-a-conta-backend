package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro/billing")
@RequiredArgsConstructor
public class FinanceiroController {

    private final BillingService billingService;

    @GetMapping("/receita")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> receita(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(billingService.relatorioReceita(inicio, fim));
    }

    @PostMapping("/bloquear-inadimplentes")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> bloquearInadimplentes() {
        billingService.bloquearContratosPorInadimplencia();
        return ResponseEntity.ok(Map.of("mensagem", "Processamento concluído"));
    }
}
