package com.oiaaconta.ifood.controller;

import com.oiaaconta.ifood.client.IfoodOrderClient;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodVinculoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Chamado pelo order-service (IfoodClient) quando o status de uma Entrega
// com origemIfood muda — traduz pro statusChange certo da Order API do
// iFood. Sem JWT, só acessível dentro da rede Docker (mesmo padrão de
// /internal/entregas no order-service).
@RestController
@RequestMapping("/internal/ifood")
@RequiredArgsConstructor
@Slf4j
public class IfoodInternalController {

    private final IfoodMerchantRepository merchantRepository;
    private final IfoodOrderClient ifoodOrderClient;
    private final IfoodVinculoService vinculoService;

    @PostMapping("/status")
    public ResponseEntity<Void> atualizarStatus(@RequestBody Map<String, Object> body) {
        Long restauranteId = ((Number) body.get("restauranteId")).longValue();
        String ifoodOrderId = (String) body.get("ifoodOrderId");
        String status = (String) body.get("status");

        IfoodMerchant merchant = merchantRepository.findByRestauranteId(restauranteId)
            .filter(IfoodMerchant::isAtivo)
            .orElse(null);
        if (merchant == null || ifoodOrderId == null) {
            log.warn("Atualização de status iFood ignorada (restaurante {}, pedido {}) — sem vínculo ativo", restauranteId, ifoodOrderId);
            return ResponseEntity.noContent().build();
        }

        String token = "Bearer " + vinculoService.garantirTokenValido(merchant);
        switch (status) {
            case "CONFIRMADA" -> ifoodOrderClient.confirmar(token, ifoodOrderId);
            case "PRONTO_PARA_ENTREGA" -> ifoodOrderClient.prontoParaEntrega(token, ifoodOrderId);
            case "SAIU_PARA_ENTREGA" -> ifoodOrderClient.saiuParaEntrega(token, ifoodOrderId);
            case "ENTREGUE" -> ifoodOrderClient.concluirEntrega(token, ifoodOrderId);
            case "CANCELADA" -> ifoodOrderClient.cancelar(token, ifoodOrderId,
                Map.of("reason", "Pedido recusado pela loja", "cancellationCode", "501"));
            default -> log.warn("Status '{}' não mapeado pra Order API do iFood", status);
        }
        return ResponseEntity.noContent().build();
    }
}
