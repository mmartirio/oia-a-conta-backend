package com.oiaaconta.ifood.controller;

import com.oiaaconta.ifood.client.IfoodOrderClient;
import com.oiaaconta.ifood.dto.ifood.IfoodMotivoCancelamentoDto;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.exception.BusinessException;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodCatalogSyncService;
import com.oiaaconta.ifood.service.IfoodVinculoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Chamado pelo order-service (IfoodClient) quando o status de uma Entrega
// com origemIfood muda — traduz pro statusChange certo da Order API do
// iFood — e pelo catalog-service (IfoodSyncClient) quando um produto/combo
// muda. Sem JWT, só acessível dentro da rede Docker (mesmo padrão de
// /internal/entregas no order-service).
@RestController
@RequestMapping("/internal/ifood")
@RequiredArgsConstructor
@Slf4j
public class IfoodInternalController {

    private final IfoodMerchantRepository merchantRepository;
    private final IfoodOrderClient ifoodOrderClient;
    private final IfoodVinculoService vinculoService;
    private final IfoodCatalogSyncService catalogSyncService;

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
            case "CONFIRMADA" -> {
                ifoodOrderClient.confirmar(token, ifoodOrderId);
                log.info("iFood: pedido {} CONFIRMADO (POST /orders/{}/confirm)", ifoodOrderId, ifoodOrderId);
                // Opcional na API do iFood — não deixa a confirmação falhar
                // se isso der erro (pedido já teria sido confirmado acima).
                try {
                    ifoodOrderClient.iniciarPreparo(token, ifoodOrderId);
                    log.info("iFood: pedido {} preparo iniciado (POST /orders/{}/startPreparation)", ifoodOrderId, ifoodOrderId);
                } catch (Exception e) {
                    log.warn("Falha ao avisar início de preparo pro iFood (pedido {}): {}", ifoodOrderId, e.getMessage());
                }
            }
            case "PRONTO_PARA_ENTREGA" -> {
                ifoodOrderClient.prontoParaEntrega(token, ifoodOrderId);
                log.info("iFood: pedido {} PRONTO PARA ENTREGA (POST /orders/{}/readyToPickup)", ifoodOrderId, ifoodOrderId);
            }
            case "SAIU_PARA_ENTREGA" -> {
                ifoodOrderClient.saiuParaEntrega(token, ifoodOrderId);
                log.info("iFood: pedido {} DESPACHADO (POST /orders/{}/dispatch)", ifoodOrderId, ifoodOrderId);
            }
            // O evento CONCLUDED é gerado automaticamente pelo iFood depois da
            // entrega — não existe endpoint pra sinalizar isso manualmente.
            case "ENTREGUE" -> log.info("iFood: pedido {} entregue — iFood conclui automaticamente, nenhuma chamada necessária", ifoodOrderId);
            case "CANCELADA" -> {
                IfoodMotivoCancelamentoDto motivo = escolherMotivoCancelamento(token, ifoodOrderId);
                ifoodOrderClient.cancelar(token, ifoodOrderId,
                    Map.of("reason", motivo.getDescription(), "cancellationCode", motivo.getCancelCodeId()));
                log.info("iFood: pedido {} CANCELADO (POST /orders/{}/requestCancellation, código {} - \"{}\")",
                    ifoodOrderId, ifoodOrderId, motivo.getCancelCodeId(), motivo.getDescription());
            }
            default -> log.warn("Status '{}' não mapeado pra Order API do iFood", status);
        }
        return ResponseEntity.noContent().build();
    }

    // O iFood só aceita, em requestCancellation, um código dentre os válidos
    // pro status atual desse pedido específico — não existe um código fixo
    // que sirva sempre (homologação rejeita cancelamento com código
    // hardcoded). Prioriza um motivo que fale de indisponibilidade da loja
    // (o caso mais comum de cancelamento pela loja); sem match, usa o
    // primeiro da lista — sempre um código válido, nunca um fixo.
    private IfoodMotivoCancelamentoDto escolherMotivoCancelamento(String token, String ifoodOrderId) {
        List<IfoodMotivoCancelamentoDto> motivos = ifoodOrderClient.motivosCancelamento(token, ifoodOrderId);
        if (motivos == null || motivos.isEmpty()) {
            throw new BusinessException("iFood não retornou motivos de cancelamento válidos pro pedido " + ifoodOrderId);
        }
        return motivos.stream()
            .filter(m -> m.getDescription() != null)
            .filter(m -> {
                String d = m.getDescription().toLowerCase();
                return d.contains("loja") || d.contains("estabelecimento") || d.contains("indispon") || d.contains("fech");
            })
            .findFirst()
            .orElse(motivos.get(0));
    }

    @PostMapping("/catalogo/sincronizar/{restauranteId}")
    public ResponseEntity<Void> sincronizarCatalogo(@PathVariable("restauranteId") Long restauranteId) {
        catalogSyncService.sincronizarSeVinculado(restauranteId);
        return ResponseEntity.noContent().build();
    }
}
