package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.VendaBalcaoRequest;
import com.oiaaconta.order.dto.response.ComandaResponse;
import com.oiaaconta.order.service.PdvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdv")
@RequiredArgsConstructor
public class PdvController {

    private final PdvService pdvService;

    @PostMapping("/vendas")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> criarVenda(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long caixaUserId,
            @RequestHeader(value = "X-User-Nome", required = false) String caixaNome,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody VendaBalcaoRequest request) {
        return ResponseEntity.status(201).body(
            pdvService.criarVenda(restauranteId, caixaUserId, caixaNome, request, authHeader));
    }
}
