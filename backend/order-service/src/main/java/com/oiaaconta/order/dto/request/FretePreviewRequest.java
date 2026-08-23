package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FretePreviewRequest {
    @NotNull(message = "Latitude do endereço obrigatória")
    private Double enderecoLatitude;

    @NotNull(message = "Longitude do endereço obrigatória")
    private Double enderecoLongitude;
}
