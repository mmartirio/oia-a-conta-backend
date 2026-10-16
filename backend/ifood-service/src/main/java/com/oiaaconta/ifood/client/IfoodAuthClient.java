package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.ifood.IfoodTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Autenticação OAuth2 do iFood — não é um serviço Spring Cloud/Eureka, URL
// fixa via propriedade (ifood.api-url).
//
// Corpo como String (já url-encoded via IfoodVinculoService.formBody), não
// MultiValueMap — o encoder padrão do Feign (SpringEncoder) não convertia o
// MultiValueMap em corpo x-www-form-urlencoded de fato (a API do iFood
// devolvia "Missing required parameter" mesmo com o campo preenchido no
// Java), então montamos a string exata na mão pra eliminar a ambiguidade.
@FeignClient(name = "ifood-auth", url = "${ifood.api-url}")
public interface IfoodAuthClient {

    // Aplicação cadastrada como Centralizada no portal do iFood — só o
    // grant client_credentials é usado (token de aplicação); sem userCode
    // nem authorization_code, que são do fluxo Distribuído.
    @PostMapping(value = "/authentication/v1.0/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    IfoodTokenResponse obterToken(@RequestBody String form);
}
