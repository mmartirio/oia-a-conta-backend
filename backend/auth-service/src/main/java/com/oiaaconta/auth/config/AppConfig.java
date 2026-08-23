package com.oiaaconta.auth.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class AppConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Valida a assinatura e o "audience" (client-id) do ID token emitido pelo
    // Google no login social — sem isso, o endpoint /api/auth/google confiaria
    // em qualquer email/nome mandado em texto puro pelo cliente.
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(@Value("${google.client-id:}") String googleClientId) {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build();
    }
}
