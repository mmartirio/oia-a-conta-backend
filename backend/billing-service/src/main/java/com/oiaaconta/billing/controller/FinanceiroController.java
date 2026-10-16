package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.service.BillingService;
import com.oiaaconta.billing.service.PainelGestorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro/billing")
@RequiredArgsConstructor
public class FinanceiroController {

    private final BillingService billingService;
    private final PainelGestorService painelGestorService;

    // Números do painel do gestor (KPIs, receita 12 meses, o que exige ação).
    @GetMapping("/resumo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> resumo() {
        return ResponseEntity.ok(painelGestorService.resumo());
    }

    // Todos os pagamentos de todas as empresas, com contrato/plano — 1 consulta.
    @GetMapping("/pagamentos")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> pagamentos() {
        return ResponseEntity.ok(painelGestorService.pagamentos());
    }

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
