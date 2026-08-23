package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.dto.request.RegistrarLogRequest;
import com.oiaaconta.billing.service.LogAuditoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoint interno (não roteado pelo api-gateway, só alcançável entre
// containers via Eureka) — usado pelos outros microserviços pra registrar
// eventos de auditoria de negócio por restaurante.
@RestController
@RequestMapping("/internal/auditoria")
@RequiredArgsConstructor
public class LogAuditoriaInternalController {

    private final LogAuditoriaService logAuditoriaService;

    @PostMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistrarLogRequest request) {
        logAuditoriaService.registrar(request);
        return ResponseEntity.ok().build();
    }
}
