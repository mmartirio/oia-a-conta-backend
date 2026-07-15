package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.EntregaRequest;
import com.oiaaconta.order.dto.response.EntregaResponse;
import com.oiaaconta.order.service.EntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Endpoint interno (serviço-a-serviço, sem JWT) para o whatsapp-service criar
// entregas a partir do chatbot/cardápio público. Requisições vindas de fora
// nunca chegam aqui — /internal/** não é roteado pelo api-gateway, só é
// alcançável via Eureka/Feign dentro da rede docker.
@RestController
@RequestMapping("/internal/entregas")
@RequiredArgsConstructor
public class EntregaInternalController {

    private final EntregaService entregaService;

    @PostMapping
    public ResponseEntity<EntregaResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody EntregaRequest request) {
        return ResponseEntity.status(201).body(entregaService.criar(restauranteId, request));
    }
}
