package com.oiaaconta.order.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Contrato enviado ao catalog-service (POST /api/estoque/verificar-baixar e
// /api/estoque/liberar) — espelha catalog-service.dto.request.EstoqueBaixaRequest.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueBaixaRequestDto {
    private List<ItemQuantidadeDto> itens;
    private String referenciaExterna;
}
