package com.oiaaconta.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "whatsapp-service")
public interface WhatsappClient {

    @PostMapping("/internal/whatsapp/notificar")
    void notificar(@RequestBody Map<String, Object> request);
}
