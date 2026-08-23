package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodTokenResponse {
    private String accessToken;
    private String refreshToken;
    private String type;
    // Segundos até expirar — nome varia entre "expiresIn" (grant novo) e
    // um inteiro em alguns retornos; mapeado como Long pra aceitar os dois.
    private Long expiresIn;
}
