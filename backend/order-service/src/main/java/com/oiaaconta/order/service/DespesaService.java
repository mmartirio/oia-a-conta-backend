package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.DespesaRequest;
import com.oiaaconta.order.dto.response.DespesaResponse;
import com.oiaaconta.order.entity.Despesa;
import com.oiaaconta.order.exception.ResourceNotFoundException;
import com.oiaaconta.order.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;

    @Transactional
    @SuppressWarnings("null")
    public DespesaResponse criar(Long restauranteId, DespesaRequest request) {
        Despesa despesa = Despesa.builder()
            .restauranteId(restauranteId)
            .categoria(request.getCategoria())
            .descricao(request.getDescricao())
            .valor(request.getValor())
            .data(request.getData())
            .build();

        return toResponse(despesaRepository.save(despesa));
    }

    public List<DespesaResponse> listar(Long restauranteId, LocalDate inicio, LocalDate fim) {
        return despesaRepository.findByRestauranteIdAndDataBetweenOrderByDataDesc(restauranteId, inicio, fim)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void excluir(Long restauranteId, Long id) {
        Despesa despesa = despesaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));
        despesaRepository.delete(despesa);
    }

    @SuppressWarnings("null")
    private DespesaResponse toResponse(Despesa d) {
        return DespesaResponse.builder()
            .id(d.getId())
            .restauranteId(d.getRestauranteId())
            .categoria(d.getCategoria().name())
            .descricao(d.getDescricao())
            .valor(d.getValor())
            .data(d.getData())
            .criadoEm(d.getCreatedAt())
            .build();
    }
}
