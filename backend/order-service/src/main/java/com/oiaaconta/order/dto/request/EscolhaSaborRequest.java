package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Um sabor escolhido pelo cliente dentro de um grupo do combo (ver
// ComboGrupoDto) — ex: produtoId=carne, quantidade=1. A soma das
// quantidades escolhidas precisa bater com a quantidade de cada grupo do
// combo (validado em EntregaService/PedidoService); sem isso (PDV/Garçom,
// que ainda não pedem sabor), cai no primeiro produto elegível de cada
// grupo como padrão.
@Data
public class EscolhaSaborRequest {
    @NotNull
    private Long produtoId;

    @NotNull @Min(1)
    private Integer quantidade;
}
