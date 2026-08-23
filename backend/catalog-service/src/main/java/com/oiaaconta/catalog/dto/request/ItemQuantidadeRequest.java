package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemQuantidadeRequest {
    @NotNull
    private Long produtoId;

    @NotNull
    @Min(1)
    private Integer quantidade;
}
