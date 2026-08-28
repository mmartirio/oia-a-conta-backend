package com.oiaaconta.whatsapp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "billing-service")
public interface BillingClient {

    @PostMapping("/internal/tickets/whatsapp")
    void registrarMensagemTicket(@RequestBody Map<String, String> body);
}
