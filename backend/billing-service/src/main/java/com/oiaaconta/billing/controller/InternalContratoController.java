package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.dto.response.PlanoLimitesResponse;
import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalContratoController {

    private final BillingService billingService;

    @PostMapping("/contratos")
    public ResponseEntity<Contrato> criarContrato(@RequestBody Map<String, Long> body) {
        Long restauranteId = body.get("restauranteId");
        Long planoId = body.get("planoId");
        return ResponseEntity.status(201).body(billingService.criarContrato(restauranteId, planoId));
    }

    // Chamado por auth-service (aplicar funcionalidades do plano no login/me)
    // e table-service (travar limite de mesas ao criar).
    @GetMapping("/contratos/{restauranteId}/limites-plano")
    public ResponseEntity<PlanoLimitesResponse> limitesPlano(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarLimitesPlano(restauranteId));
    }
}
