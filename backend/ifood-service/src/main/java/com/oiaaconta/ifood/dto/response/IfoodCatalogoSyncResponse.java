package com.oiaaconta.ifood.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IfoodCatalogoSyncResponse {
    private int categoriasSincronizadas;
    private int itensSincronizados;
    private int itensPausados;
    private LocalDateTime sincronizadoEm;
}
