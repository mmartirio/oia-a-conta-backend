package com.oiaaconta.ifood.controller;

import com.oiaaconta.ifood.dto.ifood.IfoodMerchantDto;
import com.oiaaconta.ifood.dto.response.IfoodCatalogoSyncResponse;
import com.oiaaconta.ifood.dto.response.IfoodStatusResponse;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.exception.BusinessException;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodCatalogSyncService;
import com.oiaaconta.ifood.service.IfoodVinculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ifood/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class IfoodAdminController {

    private final IfoodVinculoService vinculoService;
    private final IfoodMerchantRepository merchantRepository;
    private final IfoodCatalogSyncService catalogSyncService;

    @GetMapping("/status")
    public ResponseEntity<IfoodStatusResponse> status(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(merchantRepository.findByRestauranteId(restauranteId)
            .filter(IfoodMerchant::isAtivo)
            .map(m -> IfoodStatusResponse.builder()
                .conectado(true)
                .merchantId(m.getMerchantId())
                .merchantNome(m.getMerchantNome())
                .conectadoEm(m.getConectadoEm())
                .catalogoSincronizadoEm(m.getCatalogoSincronizadoEm())
                .build())
            .orElse(IfoodStatusResponse.builder().conectado(false).build()));
    }

    // Lojas já autorizadas pra esta aplicação no iFood (pelo Portal do
    // Parceiro) e ainda não reivindicadas por nenhum restaurante daqui — o
    // admin escolhe qual delas é a sua em POST /vincular.
    @GetMapping("/lojas-disponiveis")
    public ResponseEntity<List<IfoodMerchantDto>> lojasDisponiveis() {
        return ResponseEntity.ok(vinculoService.listarLojasDisponiveis());
    }

    @PostMapping("/vincular")
    public ResponseEntity<Void> vincular(@RequestHeader("X-Restaurante-Id") Long restauranteId,
                                          @RequestBody Map<String, String> body) {
        String merchantId = body.get("merchantId");
        if (merchantId == null || merchantId.isBlank()) {
            throw new BusinessException("merchantId é obrigatório");
        }
        vinculoService.conectar(restauranteId, merchantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/catalogo/sincronizar")
    public ResponseEntity<IfoodCatalogoSyncResponse> sincronizarCatalogo(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(catalogSyncService.sincronizar(restauranteId));
    }

    @DeleteMapping("/desconectar")
    public ResponseEntity<Void> desconectar(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        vinculoService.desconectar(restauranteId);
        return ResponseEntity.noContent().build();
    }
}
