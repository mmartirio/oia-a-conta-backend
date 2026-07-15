package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class HorarioRequest {

    @NotNull(message = "Dia da semana obrigatório")
    private DayOfWeek diaSemana;

    @NotNull(message = "Hora de abertura obrigatória")
    private LocalTime horaAbertura;

    @NotNull(message = "Hora de fechamento obrigatória")
    private LocalTime horaFechamento;
}
