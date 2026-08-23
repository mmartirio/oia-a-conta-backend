package com.oiaaconta.ifood.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IfoodStatusResponse {
    private boolean conectado;
    private String merchantId;
    private String merchantNome;
    private LocalDateTime conectadoEm;
    private LocalDateTime catalogoSincronizadoEm;
}
