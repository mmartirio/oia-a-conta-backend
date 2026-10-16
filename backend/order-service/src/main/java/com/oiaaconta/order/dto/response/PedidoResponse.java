package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PedidoResponse {
    private Long id;
    private Long comandaId;
    private Long restauranteId;
    private Long entregaId;
    private Integer mesaNumero;
    private Long garconId;
    private String garconNome;
    private String status;
    private String observacao;
    private List<ItemPedidoResponse> itens;
    private LocalDateTime criadoEm;
    private LocalDateTime prontoEm;

    // Populados a partir da Entrega vinculada (ver PedidoService.toResponse)
    // — o número que aparece pro cliente/lojista no iFood precisa bater com
    // o que a cozinha vê aqui, não o id interno do Pedido.
    private boolean origemIfood;
    private String ifoodDisplayId;
}
