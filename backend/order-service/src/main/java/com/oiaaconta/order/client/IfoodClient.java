package com.oiaaconta.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

// Espelha o WhatsappClient — efeito colateral best-effort (chamada
// engolida em try/catch pelo call-site), nunca deve travar a mudança de
// status local por uma falha na integração externa.
@FeignClient(name = "ifood-service")
public interface IfoodClient {

    @PostMapping("/internal/ifood/status")
    void atualizarStatusPedido(@RequestBody Map<String, Object> request);
}
