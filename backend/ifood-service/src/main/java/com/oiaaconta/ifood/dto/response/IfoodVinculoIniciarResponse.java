package com.oiaaconta.ifood.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IfoodVinculoIniciarResponse {
    private String userCode;
    private String verificationUrl;
    private String verificationUrlComplete;
    private Integer expiresIn;
}
