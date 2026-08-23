package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Resposta de POST /authentication/v1.0/oauth/userCode — primeiro passo do
// vínculo de loja: o admin acessa verificationUrlComplete e autoriza a
// aplicação; authorizationCodeVerifier é reapresentado na troca do token
// (ver IfoodAuthClient.obterToken).
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodUserCodeResponse {
    private String userCode;
    private String authorizationCodeVerifier;
    private String verificationUrl;
    private String verificationUrlComplete;
    private Integer expiresIn;
}
