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

    // Chamado pelo ifood-service quando chega um evento CANCELLED via
    // polling (cancelamento decidido do lado do iFood/cliente) — só atualiza
    // o status local, sem notificar o iFood de volta.
    @PostMapping("/ifood/{ifoodOrderId}/cancelar")
    public ResponseEntity<Void> cancelarOrigemIfood(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String ifoodOrderId) {
        entregaService.cancelarOrigemIfood(restauranteId, ifoodOrderId);
        return ResponseEntity.noContent().build();
    }

    // Chamado pelo ifood-service quando chega um evento CONCLUDED via
    // polling — gerado automaticamente pelo iFood depois da entrega.
    @PostMapping("/ifood/{ifoodOrderId}/concluir")
    public ResponseEntity<Void> concluirOrigemIfood(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String ifoodOrderId) {
        entregaService.concluirOrigemIfood(restauranteId, ifoodOrderId);
        return ResponseEntity.noContent().build();
    }
}
