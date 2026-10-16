package com.oiaaconta.ifood.dto.ifood;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Corpo do PUT /merchants/{merchantId}/items (Catalog v2) — chamada única,
// idempotente por item.id, que cria OU atualiza o item (não existe POST/PUT
// separado por id no path; reenviar o mesmo item.id substitui o anterior).
// O "product" embutido também é criado/atualizado junto, sem endpoint
// próprio — por isso os dois vêm sempre juntos aqui.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IfoodItemRequest {
    private Item item;
    private List<Produto> products;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private String id;
        // DEFAULT | PIZZA | COMBO_V2 — nosso catálogo só usa DEFAULT
        private String type;
        private String categoryId;
        // AVAILABLE | UNAVAILABLE
        private String status;
        private Preco price;
        private String externalCode;
        private String productId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Produto {
        private String id;
        private String name;
        private String description;
        private String externalCode;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Preco {
        private BigDecimal value;
    }
}
