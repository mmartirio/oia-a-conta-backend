package com.oiaaconta.order.dto.request;

import com.oiaaconta.order.enums.TipoDescontoOrigem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AplicarDescontoRequest {
    @NotNull(message = "Tipo obrigatório")
    private TipoDescontoOrigem tipo; // CUPOM ou PROMOCAO

    // Obrigatório sse tipo=CUPOM.
    private String codigo;

    // Obrigatório sse tipo=PROMOCAO — precisa estar na lista de promoções
    // aplicáveis retornada por GET /api/promocoes/aplicaveis (revalidado
    // server-side em ComandaService, nunca confia em desconto vindo do cliente).
    private Long promocaoId;
}
