package com.comandadigital.table.service;

import com.comandadigital.table.dto.request.MesaRequest;
import com.comandadigital.table.dto.response.MesaResponse;
import com.comandadigital.table.entity.Mesa;
import com.comandadigital.table.enums.StatusMesa;
import com.comandadigital.table.exception.BusinessException;
import com.comandadigital.table.exception.ResourceNotFoundException;
import com.comandadigital.table.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository mesaRepository;

    public List<MesaResponse> listar(Long restauranteId) {
        return mesaRepository.findByRestauranteIdOrderByNumeroAsc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public MesaResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(mesaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa não encontrada")));
    }

    public MesaResponse criar(Long restauranteId, MesaRequest request) {
        if (mesaRepository.existsByRestauranteIdAndNumero(restauranteId, request.getNumero())) {
            throw new BusinessException("Mesa " + request.getNumero() + " já existe");
        }
        Mesa mesa = mesaRepository.save(Mesa.builder()
            .restauranteId(restauranteId)
            .numero(request.getNumero())
            .capacidade(request.getCapacidade() != null ? request.getCapacidade() : 4)
            .status(StatusMesa.DISPONIVEL)
            .build());
        return toResponse(mesa);
    }

    public MesaResponse atualizar(Long restauranteId, Long id, MesaRequest request) {
        Mesa mesa = mesaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa não encontrada"));
        if (!mesa.getNumero().equals(request.getNumero()) &&
                mesaRepository.existsByRestauranteIdAndNumero(restauranteId, request.getNumero())) {
            throw new BusinessException("Mesa " + request.getNumero() + " já existe");
        }
        mesa.setNumero(request.getNumero());
        if (request.getCapacidade() != null) mesa.setCapacidade(request.getCapacidade());
        return toResponse(mesaRepository.save(mesa));
    }

    public MesaResponse atualizarStatus(Long restauranteId, Long id, StatusMesa status) {
        Mesa mesa = mesaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa não encontrada"));
        mesa.setStatus(status);
        return toResponse(mesaRepository.save(mesa));
    }

    public void excluir(Long restauranteId, Long id) {
        Mesa mesa = mesaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa não encontrada"));
        if (mesa.getStatus() != StatusMesa.DISPONIVEL) {
            throw new BusinessException("Não é possível excluir mesa que não está disponível");
        }
        mesaRepository.delete(mesa);
    }

    private MesaResponse toResponse(Mesa m) {
        return MesaResponse.builder()
            .id(m.getId()).restauranteId(m.getRestauranteId())
            .numero(m.getNumero()).capacidade(m.getCapacidade())
            .status(m.getStatus().name()).build();
    }
}
