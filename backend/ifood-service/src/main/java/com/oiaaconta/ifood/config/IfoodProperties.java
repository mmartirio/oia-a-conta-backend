package com.oiaaconta.ifood.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Credenciais da aplicação (Client ID/Secret), cadastradas uma vez no portal
// de desenvolvedores do iFood — compartilhadas por toda a plataforma, não
// por restaurante (ver IfoodVinculoService pro vínculo por restaurante).
@Component
public class IfoodProperties {

    @Value("${ifood.api-url}")
    private String apiUrl;

    @Value("${ifood.client-id}")
    private String clientId;

    @Value("${ifood.client-secret}")
    private String clientSecret;

    public String getApiUrl() { return apiUrl; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }

    public boolean configurada() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
