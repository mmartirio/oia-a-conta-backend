package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PausaProgramadaRequest {
    @NotBlank(message = "Título obrigatório")
    private String titulo;

    @NotNull(message = "Início obrigatório")
    private LocalDateTime inicio;

    @NotNull(message = "Fim obrigatório")
    private LocalDateTime fim;
}
