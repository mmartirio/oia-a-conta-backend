package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.ifood.IfoodInterrupcaoRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodInterrupcaoResponse;
import com.oiaaconta.ifood.dto.ifood.IfoodMerchantDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ifood-merchant", url = "${ifood.api-url}")
public interface IfoodMerchantClient {

    @GetMapping("/merchant/v1.0/merchants")
    List<IfoodMerchantDto> listarMerchants(@RequestHeader("Authorization") String bearerToken);

    // { "available": true/false, "message": [...] } — usado pra decidir se
    // abre/fecha a loja na sincronização de status (ver IfoodStatusSyncService).
    @GetMapping("/merchant/v1.0/merchants/{merchantId}/status")
    Map<String, Object> statusMerchant(@RequestHeader("Authorization") String bearerToken, @PathVariable("merchantId") String merchantId);

    // "Fechar agora" no iFood é criar uma interrupção temporária (sem
    // interrupção ativa = loja aberta pro app deles); reabrir é remover
    // essa mesma interrupção antes do fim programado.
    @PostMapping("/merchant/v1.0/merchants/{merchantId}/interruptions")
    IfoodInterrupcaoResponse criarInterrupcao(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @RequestBody IfoodInterrupcaoRequest body);

    @DeleteMapping("/merchant/v1.0/merchants/{merchantId}/interruptions/{interruptionId}")
    void removerInterrupcao(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("interruptionId") String interruptionId);
}
