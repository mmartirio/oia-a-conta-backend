package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.AbrirCaixaRequest;
import com.oiaaconta.order.dto.request.FecharCaixaRequest;
import com.oiaaconta.order.dto.response.SessaoCaixaResponse;
import com.oiaaconta.order.service.CaixaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caixa")
@RequiredArgsConstructor
public class CaixaController {

    private final CaixaService caixaService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SessaoCaixaResponse> status(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(caixaService.status(restauranteId));
    }

    @GetMapping("/historico")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<SessaoCaixaResponse>> historico(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(caixaService.historico(restauranteId));
    }

    @PostMapping("/abrir")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SessaoCaixaResponse> abrir(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long usuarioId,
            @RequestHeader("X-User-Nome") String usuarioNome,
            @Valid @RequestBody AbrirCaixaRequest request) {
        return ResponseEntity.status(201).body(caixaService.abrir(restauranteId, usuarioId, usuarioNome, request));
    }

    @PostMapping("/fechar")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SessaoCaixaResponse> fechar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long usuarioId,
            @RequestHeader("X-User-Nome") String usuarioNome,
            @Valid @RequestBody FecharCaixaRequest request) {
        return ResponseEntity.ok(caixaService.fechar(restauranteId, usuarioId, usuarioNome, request));
    }
}
