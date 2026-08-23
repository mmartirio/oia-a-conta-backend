package com.oiaaconta.order.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemQuantidadeDto {
    private Long produtoId;
    private Integer quantidade;
}
