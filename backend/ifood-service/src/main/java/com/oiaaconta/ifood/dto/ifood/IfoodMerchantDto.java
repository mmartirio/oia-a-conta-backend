package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Item de GET /merchant/v1.0/merchants — cada loja liberada pra essa
// aplicação dentro da conta iFood que o admin autorizou.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodMerchantDto {
    private String id;
    private String name;
    private String corporateName;
}
