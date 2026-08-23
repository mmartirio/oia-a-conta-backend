package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReordenarCategoriasRequest {
    @NotEmpty(message = "Informe ao menos uma categoria")
    private List<Long> ids;
}
