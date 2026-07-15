package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
public class HorarioResponse {
    private Long id;
    private DayOfWeek diaSemana;
    private LocalTime horaAbertura;
    private LocalTime horaFechamento;
}
