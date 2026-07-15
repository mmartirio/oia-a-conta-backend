package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RestauranteLocalizacaoRequest {
    @NotNull(message = "Latitude obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude obrigatória")
    private Double longitude;
}
