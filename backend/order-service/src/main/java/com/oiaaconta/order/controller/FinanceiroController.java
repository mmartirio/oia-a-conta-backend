package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.response.ComissaoResponse;
import com.oiaaconta.order.dto.response.ResumoFinanceiroResponse;
import com.oiaaconta.order.service.FinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<ResumoFinanceiroResponse> resumo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.getResumo(restauranteId, dataInicio, dataFim));
    }

    @GetMapping("/comissoes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ComissaoResponse>> comissoes(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.getComissoes(restauranteId, dataInicio, dataFim));
    }
}
