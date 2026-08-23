package com.oiaaconta.ifood.controller;

import com.oiaaconta.ifood.dto.ifood.IfoodUserCodeResponse;
import com.oiaaconta.ifood.dto.response.IfoodCatalogoSyncResponse;
import com.oiaaconta.ifood.dto.response.IfoodStatusResponse;
import com.oiaaconta.ifood.dto.response.IfoodVinculoIniciarResponse;
import com.oiaaconta.ifood.dto.response.IfoodVinculoStatusResponse;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodCatalogSyncService;
import com.oiaaconta.ifood.service.IfoodVinculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/vincular")
    public ResponseEntity<IfoodVinculoIniciarResponse> vincular(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        IfoodUserCodeResponse resp = vinculoService.iniciar(restauranteId);
        return ResponseEntity.ok(IfoodVinculoIniciarResponse.builder()
            .userCode(resp.getUserCode())
            .verificationUrl(resp.getVerificationUrl())
            .verificationUrlComplete(resp.getVerificationUrlComplete())
            .expiresIn(resp.getExpiresIn())
            .build());
    }

    @GetMapping("/vincular/status")
    public ResponseEntity<IfoodVinculoStatusResponse> vincularStatus(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(IfoodVinculoStatusResponse.builder()
            .status(vinculoService.verificar(restauranteId))
            .build());
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
