package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.GrupoClienteRequest;
import com.oiaaconta.catalog.dto.response.GrupoClienteMembroResponse;
import com.oiaaconta.catalog.dto.response.GrupoClienteResponse;
import com.oiaaconta.catalog.entity.Cliente;
import com.oiaaconta.catalog.entity.GrupoCliente;
import com.oiaaconta.catalog.entity.GrupoClienteMembro;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.ClienteRepository;
import com.oiaaconta.catalog.repository.GrupoClienteMembroRepository;
import com.oiaaconta.catalog.repository.GrupoClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrupoClienteService {

    private final GrupoClienteRepository grupoClienteRepository;
    private final GrupoClienteMembroRepository membroRepository;
    private final ClienteRepository clienteRepository;

    public List<GrupoClienteResponse> listar(Long restauranteId, boolean apenasAtivos) {
        List<GrupoCliente> grupos = apenasAtivos
            ? grupoClienteRepository.findByRestauranteIdAndAtivoTrueOrderByNomeAsc(restauranteId)
            : grupoClienteRepository.findByRestauranteIdOrderByNomeAsc(restauranteId);
        return grupos.stream().map(this::toResponse).toList();
    }

    public GrupoClienteResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(buscarEntidade(restauranteId, id));
    }

    public GrupoClienteResponse criar(Long restauranteId, GrupoClienteRequest request) {
        GrupoCliente grupo = grupoClienteRepository.save(GrupoCliente.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome())
            .descricao(request.getDescricao())
            .ativo(true)
            .build());
        return toResponse(grupo);
    }

    public GrupoClienteResponse atualizar(Long restauranteId, Long id, GrupoClienteRequest request) {
        GrupoCliente grupo = buscarEntidade(restauranteId, id);
        grupo.setNome(request.getNome());
        grupo.setDescricao(request.getDescricao());
        return toResponse(grupoClienteRepository.save(grupo));
    }

    public GrupoClienteResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        GrupoCliente grupo = buscarEntidade(restauranteId, id);
        grupo.setAtivo(ativo);
        return toResponse(grupoClienteRepository.save(grupo));
    }

    public List<GrupoClienteMembroResponse> listarMembros(Long restauranteId, Long grupoId) {
        buscarEntidade(restauranteId, grupoId);
        return membroRepository.findByGrupoClienteIdAndRestauranteId(grupoId, restauranteId).stream()
            .map(m -> {
                Cliente cliente = clienteRepository.findByIdAndRestauranteId(m.getClienteId(), restauranteId).orElse(null);
                return GrupoClienteMembroResponse.builder()
                    .clienteId(m.getClienteId())
                    .clienteNome(cliente != null ? cliente.getNome() : null)
                    .clienteTelefone(cliente != null ? cliente.getTelefone() : null)
                    .adicionadoEm(m.getAdicionadoEm())
                    .build();
            })
            .toList();
    }

    public void adicionarMembro(Long restauranteId, Long grupoId, Long clienteId) {
        buscarEntidade(restauranteId, grupoId);
        clienteRepository.findByIdAndRestauranteId(clienteId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        if (membroRepository.existsByGrupoClienteIdAndClienteId(grupoId, clienteId)) {
            throw new BusinessException("Cliente já pertence a este grupo");
        }
        membroRepository.save(GrupoClienteMembro.builder()
            .restauranteId(restauranteId)
            .grupoClienteId(grupoId)
            .clienteId(clienteId)
            .build());
    }

    public void removerMembro(Long restauranteId, Long grupoId, Long clienteId) {
        buscarEntidade(restauranteId, grupoId);
        GrupoClienteMembro membro = membroRepository
            .findByGrupoClienteIdAndClienteIdAndRestauranteId(grupoId, clienteId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não pertence a este grupo"));
        membroRepository.delete(membro);
    }

    GrupoCliente buscarEntidade(Long restauranteId, Long id) {
        return grupoClienteRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Grupo de clientes não encontrado"));
    }

    private GrupoClienteResponse toResponse(GrupoCliente g) {
        return GrupoClienteResponse.builder()
            .id(g.getId()).restauranteId(g.getRestauranteId())
            .nome(g.getNome()).descricao(g.getDescricao()).ativo(g.isAtivo())
            .totalMembros(membroRepository.countByGrupoClienteId(g.getId()))
            .createdAt(g.getCreatedAt())
            .build();
    }
}
