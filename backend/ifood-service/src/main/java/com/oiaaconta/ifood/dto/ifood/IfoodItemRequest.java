package com.oiaaconta.ifood.dto.ifood;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IfoodItemRequest {
    private String externalCode;
    private String categoryId;
    private String name;
    private String description;
    private Preco price;
    // AVAILABLE | UNAVAILABLE
    private String status;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Preco {
        private BigDecimal value;
    }
}
