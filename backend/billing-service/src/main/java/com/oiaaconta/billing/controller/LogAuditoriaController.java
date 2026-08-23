package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.dto.response.LogAuditoriaResponse;
import com.oiaaconta.billing.service.LogAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class LogAuditoriaController {

    private final LogAuditoriaService logAuditoriaService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<LogAuditoriaResponse>> listar(
            @RequestParam Long restauranteId,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(logAuditoriaService.listar(restauranteId, tipo, pageable));
    }
}
