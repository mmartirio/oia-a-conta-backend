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

    // Resposta é uma LISTA (um item por salesChannel/operation, ex:
    // "ifood-app"/"delivery") — cada item tem "available" e "message.title"
    // (ex: "Loja aberta"). Confirmado contra a API real: não é um objeto
    // único como o comentário anterior descrevia. Não usado hoje (o
    // controle de aberto/fechado é todo local, ver IfoodStatusSyncService),
    // mantido pra eventual exibição desse status no admin.
    @GetMapping("/merchant/v1.0/merchants/{merchantId}/status")
    List<Map<String, Object>> statusMerchant(@RequestHeader("Authorization") String bearerToken, @PathVariable("merchantId") String merchantId);

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
