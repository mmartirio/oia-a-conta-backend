package com.oiaaconta.order.enums;

public enum StatusEntrega {
    // Pedido do cardápio com pagamento online ainda não aprovado pelo
    // Mercado Pago — não aparece pro restaurante nem vai pra cozinha.
    AGUARDANDO_PAGAMENTO,
    AGUARDANDO,
    CONFIRMADA,
    ACEITA,
    PRONTO_PARA_ENTREGA,
    SAIU_PARA_ENTREGA,
    ENTREGUE,
    CANCELADA
}
