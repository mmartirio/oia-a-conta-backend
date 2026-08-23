package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Resposta de GET /order/v1.0/orders/{id} — só os campos usados pra montar
// a Entrega local (ver IfoodEventsPoller); a resposta real do iFood tem
// muito mais informação (pagamentos, totais detalhados, etc.), ignorada
// aqui via @JsonIgnoreProperties.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodPedidoDto {
    private String id;
    private String displayId;
    private Cliente customer;
    private Entrega delivery;
    private List<Item> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cliente {
        private String name;
        private Telefone phone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Telefone {
        private String number;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entrega {
        private Endereco deliveryAddress;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Endereco {
        private String streetName;
        private String streetNumber;
        private String neighborhood;
        private String city;
        private String complement;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String name;
        // Preenchido com o mesmo valor que gravamos na sincronização de
        // catálogo ("produto-123"/"combo-45") — é assim que resolvemos o
        // item do pedido de volta pro nosso produtoId/comboId
        // (IfoodMapeamento, tipo+ifoodId).
        private String externalCode;
        private Integer quantity;
        private Preco unitPrice;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Preco {
        private BigDecimal value;
    }
}
