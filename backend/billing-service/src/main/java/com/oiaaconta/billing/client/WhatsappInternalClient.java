package com.oiaaconta.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "whatsapp-service")
public interface WhatsappInternalClient {

    @PostMapping("/internal/whatsapp/enviar-suporte")
    void enviarSuporte(@RequestBody Map<String, String> body);
}
