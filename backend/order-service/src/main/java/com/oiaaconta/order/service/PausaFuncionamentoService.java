package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.response.PausaResponse;
import com.oiaaconta.order.dto.response.StatusFuncionamentoResponse;
import com.oiaaconta.order.entity.HorarioFuncionamento;
import com.oiaaconta.order.entity.PausaFuncionamento;
import com.oiaaconta.order.entity.RestauranteConfig;
import com.oiaaconta.order.enums.TipoPausa;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.exception.ResourceNotFoundException;
import com.oiaaconta.order.repository.HorarioFuncionamentoRepository;
import com.oiaaconta.order.repository.PausaFuncionamentoRepository;
import com.oiaaconta.order.repository.RestauranteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PausaFuncionamentoService {

    private final PausaFuncionamentoRepository pausaRepository;
    private final RestauranteConfigRepository restauranteConfigRepository;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;

    @Transactional
    public PausaResponse criarProgramada(Long restauranteId, String titulo, LocalDateTime inicio, LocalDateTime fim) {
        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("Título obrigatório para pausa programada");
        }
        if (fim == null || inicio == null || !fim.isAfter(inicio)) {
            throw new BusinessException("O fim da pausa deve ser depois do início");
        }

        PausaFuncionamento pausa = PausaFuncionamento.builder()
            .restauranteId(restauranteId)
            .tipo(TipoPausa.PROGRAMADA)
            .titulo(titulo)
            .inicio(inicio)
            .fim(fim)
            .build();

        return toResponse(pausaRepository.save(pausa));
    }

    @Transactional
    public PausaResponse criarEncerramentoAntecipado(Long restauranteId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Motivo obrigatório para encerramento antecipado");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime fimDoDia = LocalDateTime.of(agora.toLocalDate(), LocalTime.of(23, 59, 59));

        PausaFuncionamento pausa = PausaFuncionamento.builder()
            .restauranteId(restauranteId)
            .tipo(TipoPausa.ENCERRAMENTO_ANTECIPADO)
            .motivo(motivo)
            .inicio(agora)
            .fim(fimDoDia)
            .build();

        return toResponse(pausaRepository.save(pausa));
    }

    public List<PausaResponse> listar(Long restauranteId) {
        return pausaRepository.findByRestauranteIdAndFimAfterOrderByInicioAsc(restauranteId, LocalDateTime.now())
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void cancelar(Long restauranteId, Long id) {
        PausaFuncionamento pausa = pausaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Pausa não encontrada"));
        pausaRepository.delete(pausa);
    }

    public StatusFuncionamentoResponse getStatus(Long restauranteId) {
        LocalDateTime agora = LocalDateTime.now();

        // 1) Fechamento manual tem prioridade máxima: independe de pausa/horário.
        Optional<RestauranteConfig> config = restauranteConfigRepository.findByRestauranteId(restauranteId);
        boolean entregadorExterno = config.map(RestauranteConfig::isEntregadorExterno).orElse(false);
        if (config.isPresent() && config.get().isFechadoManualmente()) {
            String motivo = config.get().getMotivoFechamentoManual();
            return StatusFuncionamentoResponse.builder()
                .aberto(false)
                .motivo(motivo != null && !motivo.isBlank() ? motivo : "Fechado manualmente")
                .reaberturaPrevista(null)
                .entregadorExterno(entregadorExterno)
                .build();
        }

        // 2) Pausa ativa (programada ou encerramento antecipado) — comportamento já existente.
        Optional<StatusFuncionamentoResponse> statusPausa = pausaRepository
            .findByRestauranteIdAndFimAfterOrderByInicioAsc(restauranteId, agora).stream()
            .filter(p -> !p.getInicio().isAfter(agora))
            .findFirst()
            .map(p -> StatusFuncionamentoResponse.builder()
                .aberto(false)
                .motivo(p.getTitulo() != null && !p.getTitulo().isBlank() ? p.getTitulo() : p.getMotivo())
                .reaberturaPrevista(p.getFim())
                .entregadorExterno(entregadorExterno)
                .build());
        if (statusPausa.isPresent()) {
            return statusPausa.get();
        }

        // 3) Horário semanal configurado.
        return statusPorHorarioSemanal(restauranteId, agora, entregadorExterno);
    }

    private StatusFuncionamentoResponse statusPorHorarioSemanal(Long restauranteId, LocalDateTime agora, boolean entregadorExterno) {
        List<HorarioFuncionamento> horarios =
            horarioFuncionamentoRepository.findByRestauranteIdOrderByDiaSemanaAscHoraAberturaAsc(restauranteId);

        // Restaurante nunca configurou horário semanal: mantém comportamento atual (aberto).
        if (horarios.isEmpty()) {
            return StatusFuncionamentoResponse.builder().aberto(true).entregadorExterno(entregadorExterno).build();
        }

        DayOfWeek hoje = agora.getDayOfWeek();
        LocalTime horaAtual = agora.toLocalTime();

        boolean dentroDoExpediente = horarios.stream().anyMatch(h ->
            h.getDiaSemana() == hoje
                && !horaAtual.isBefore(h.getHoraAbertura())
                && horaAtual.isBefore(h.getHoraFechamento()));

        if (dentroDoExpediente) {
            return StatusFuncionamentoResponse.builder().aberto(true).entregadorExterno(entregadorExterno).build();
        }

        LocalDateTime reaberturaPrevista = calcularProximaAbertura(horarios, agora);
        return StatusFuncionamentoResponse.builder()
            .aberto(false)
            .motivo("Fora do horário de funcionamento")
            .reaberturaPrevista(reaberturaPrevista)
            .entregadorExterno(entregadorExterno)
            .build();
    }

    private LocalDateTime calcularProximaAbertura(List<HorarioFuncionamento> horarios, LocalDateTime agora) {
        try {
            DayOfWeek hoje = agora.getDayOfWeek();
            LocalTime horaAtual = agora.toLocalTime();
            LocalDate hojeData = agora.toLocalDate();

            // Ainda há algum intervalo hoje que começa mais tarde?
            Optional<LocalTime> proximaHoraHoje = horarios.stream()
                .filter(h -> h.getDiaSemana() == hoje && h.getHoraAbertura().isAfter(horaAtual))
                .map(HorarioFuncionamento::getHoraAbertura)
                .min(Comparator.naturalOrder());
            if (proximaHoraHoje.isPresent()) {
                return LocalDateTime.of(hojeData, proximaHoraHoje.get());
            }

            // Procura o próximo dia (a partir de amanhã, até 7 dias à frente) com algum horário configurado.
            for (int i = 1; i <= 7; i++) {
                DayOfWeek diaCandidato = hoje.plus(i);
                LocalDate dataCandidata = hojeData.plusDays(i);
                final DayOfWeek diaFinal = diaCandidato;
                Optional<LocalTime> primeiraHoraDoDia = horarios.stream()
                    .filter(h -> h.getDiaSemana() == diaFinal)
                    .map(HorarioFuncionamento::getHoraAbertura)
                    .min(Comparator.naturalOrder());
                if (primeiraHoraDoDia.isPresent()) {
                    return LocalDateTime.of(dataCandidata, primeiraHoraDoDia.get());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private PausaResponse toResponse(PausaFuncionamento p) {
        return PausaResponse.builder()
            .id(p.getId())
            .restauranteId(p.getRestauranteId())
            .tipo(p.getTipo().name())
            .titulo(p.getTitulo())
            .motivo(p.getMotivo())
            .inicio(p.getInicio())
            .fim(p.getFim())
            .build();
    }
}
