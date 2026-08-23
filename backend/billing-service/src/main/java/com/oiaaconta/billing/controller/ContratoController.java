package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.enums.StatusContrato;
import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<Contrato>> listar(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(billingService.listarContratos(pageable));
    }

    @GetMapping("/meu")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Contrato> meuContrato(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarContratoDoRestaurante(restauranteId));
    }

    @GetMapping("/restaurante/{restauranteId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> buscarPorRestaurante(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarContratoDoRestaurante(restauranteId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> criar(@RequestBody Map<String, Long> body) {
        Long restauranteId = body.get("restauranteId");
        Long planoId = body.get("planoId");
        return ResponseEntity.status(201).body(billingService.criarContrato(restauranteId, planoId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> atualizarStatus(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        StatusContrato status = StatusContrato.valueOf(body.get("status"));
        return ResponseEntity.ok(billingService.atualizarStatusContrato(id, status));
    }

    @PostMapping("/{id}/pagamento-manual")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Pagamento> pagamentoManual(@PathVariable Long id,
                                                      @RequestBody Map<String, Object> body) {
        BigDecimal valor = new BigDecimal(body.get("valor").toString());
        String obs = body.getOrDefault("observacao", "").toString();
        return ResponseEntity.status(201).body(billingService.registrarPagamentoManual(id, valor, obs));
    }

    @GetMapping("/{id}/pagamentos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Page<Pagamento>> pagamentos(@PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(billingService.listarPagamentosDoContrato(id, pageable));
    }

    // Restaurante sem contrato (ex: cadastrado fora do fluxo normal de
    // registro) — resposta limpa de "não encontrado" em vez de deixar a
    // exceção não tratada propagar como erro genérico.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNaoEncontrado() {
        return ResponseEntity.notFound().build();
    }
}
