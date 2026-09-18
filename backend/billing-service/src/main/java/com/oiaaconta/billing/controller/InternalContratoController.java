package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.dto.response.PlanoLimitesResponse;
import com.oiaaconta.billing.dto.response.RestricoesOperacaoResponse;
import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.enums.ModalidadeOperacao;
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
    public ResponseEntity<Contrato> criarContrato(@RequestBody Map<String, Object> body) {
        Long restauranteId = ((Number) body.get("restauranteId")).longValue();
        Long planoId = ((Number) body.get("planoId")).longValue();
        ModalidadeOperacao modalidade = body.get("modalidadeOperacao") != null
            ? ModalidadeOperacao.valueOf(body.get("modalidadeOperacao").toString())
            : null;
        return ResponseEntity.status(201).body(billingService.criarContrato(restauranteId, planoId, modalidade));
    }

    // Chamado por auth-service (aplicar funcionalidades do plano no login/me)
    // e table-service (travar limite de mesas ao criar).
    @GetMapping("/contratos/{restauranteId}/limites-plano")
    public ResponseEntity<PlanoLimitesResponse> limitesPlano(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarLimitesPlano(restauranteId));
    }

    // Consultado por table-service, order-service, ifood-service e
    // auth-service pra saber o que bloquear/limitar por causa da modalidade
    // de operação e do plano do restaurante — tudo já resolvido aqui, pra
    // cada serviço não precisar replicar a lógica de modalidade.
    @GetMapping("/contratos/restaurante/{restauranteId}/restricoes")
    public ResponseEntity<RestricoesOperacaoResponse> buscarRestricoes(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarRestricoesOperacao(restauranteId));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Void> handleNaoEncontrado() {
        return ResponseEntity.notFound().build();
    }
}
