package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.HorarioRequest;
import com.oiaaconta.order.dto.response.HorarioResponse;
import com.oiaaconta.order.entity.HorarioFuncionamento;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.repository.HorarioFuncionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioFuncionamentoService {

    private final HorarioFuncionamentoRepository horarioRepository;

    public List<HorarioResponse> listar(Long restauranteId) {
        return horarioRepository.findByRestauranteIdOrderByDiaSemanaAscHoraAberturaAsc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<HorarioResponse> salvarSemana(Long restauranteId, List<HorarioRequest> horarios) {
        for (HorarioRequest h : horarios) {
            if (h.getHoraAbertura().equals(h.getHoraFechamento())) {
                throw new BusinessException("Hora de abertura e fechamento não podem ser iguais");
            }
            // Simplificação (v1): não suportamos horário que vira a madrugada (ex.: 22h-02h).
            // Nesse caso horaFechamento < horaAbertura seria necessário para representar a virada
            // de dia; por ora exigimos horaFechamento > horaAbertura. Suporte a virada de dia
            // fica para uma versão futura.
            if (h.getHoraFechamento().isBefore(h.getHoraAbertura())) {
                throw new BusinessException(
                    "Horário que vira a madrugada (fechamento antes da abertura) ainda não é suportado");
            }
        }

        horarioRepository.deleteByRestauranteId(restauranteId);

        List<HorarioFuncionamento> novos = horarios.stream()
            .map(h -> HorarioFuncionamento.builder()
                .restauranteId(restauranteId)
                .diaSemana(h.getDiaSemana())
                .horaAbertura(h.getHoraAbertura())
                .horaFechamento(h.getHoraFechamento())
                .build())
            .toList();

        return horarioRepository.saveAll(novos)
            .stream().map(this::toResponse).toList();
    }

    private HorarioResponse toResponse(HorarioFuncionamento h) {
        return HorarioResponse.builder()
            .id(h.getId())
            .diaSemana(h.getDiaSemana())
            .horaAbertura(h.getHoraAbertura())
            .horaFechamento(h.getHoraFechamento())
            .build();
    }
}
