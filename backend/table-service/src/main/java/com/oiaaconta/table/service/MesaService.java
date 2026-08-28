package com.oiaaconta.table.service;

import com.oiaaconta.table.client.BillingClient;
import com.oiaaconta.table.dto.request.MesaRequest;
import com.oiaaconta.table.dto.response.MesaResponse;
import com.oiaaconta.table.entity.Mesa;
import com.oiaaconta.table.enums.StatusMesa;
import com.oiaaconta.table.exception.BusinessException;
import com.oiaaconta.table.exception.ResourceNotFoundException;
import com.oiaaconta.table.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository mesaRepository;
    private final BillingClient billingClient;

    public List<MesaResponse> listar(Long restauranteId) {
        return mesaRepository.findByRestauranteIdOrderByNumeroAsc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public MesaResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(mesaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa não encontrada")));
    }

    @SuppressWarnings("null")
    public MesaResponse criar(Long restauranteId, MesaRequest request) {
        if (mesaRepository.existsByRestauranteIdAndNumero(restauranteId, request.getNumero())) {
            throw new BusinessException("Mesa " + request.getNumero() + " já existe");
        }
        verificarLimiteMesas(restauranteId);
        Mesa mesa = mesaRepository.save(Mesa.builder()
            .restauranteId(restauranteId)
            .numero(request.getNumero())
            .capacidade(request.getCapacidade() != null ? request.getCapacidade() : 4)
            .status(StatusMesa.DISPONIVEL)
            .build());
        return toResponse(mesa);
    }

    // Sem contrato/plano ou com o billing-service fora do ar, não bloqueia
    // (best-effort) — mesma postura do limite de usuários no auth-service.
    private void verificarLimiteMesas(Long restauranteId) {
        Integer limite;
        try {
            limite = billingClient.buscarLimitesPlano(restauranteId).getLimiteMesas();
        } catch (Exception e) {
            return;
        }
        if (limite == null) return;
        if (mesaRepository.countByRestauranteId(restauranteId) >= limite) {
            throw new BusinessException(
                "Limite de " + limite + " mesa(s) do plano contratado atingido. Faça upgrade do plano para adicionar mais mesas.");
        }
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
