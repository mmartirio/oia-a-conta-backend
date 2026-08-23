package com.oiaaconta.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

// Contrato interno consumido pelo order-service (via Feign) no momento da
// venda, pra verificar+baixar (ou devolver) estoque em lote e de forma atômica.
@Data
public class EstoqueBaixaRequest {
    @NotEmpty(message = "Lista de itens obrigatória")
    @Valid
    private List<ItemQuantidadeRequest> itens;

    // ex: "pedido:123" — correlaciona com order-service pra permitir estorno depois.
    private String referenciaExterna;
}
