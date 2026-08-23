package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.ClienteRequest;
import com.oiaaconta.catalog.dto.response.ClienteResponse;
import com.oiaaconta.catalog.entity.Cliente;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public List<ClienteResponse> listar(Long restauranteId, boolean apenasAtivos) {
        List<Cliente> clientes = apenasAtivos
            ? clienteRepository.findByRestauranteIdAndAtivoTrueOrderByNomeAsc(restauranteId)
            : clienteRepository.findByRestauranteIdOrderByNomeAsc(restauranteId);
        return clientes.stream().map(this::toResponse).toList();
    }

    public ClienteResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(buscarEntidade(restauranteId, id));
    }

    public ClienteResponse buscarPorTelefone(Long restauranteId, String telefone) {
        return clienteRepository.findByRestauranteIdAndTelefone(restauranteId, telefone)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }

    public ClienteResponse criar(Long restauranteId, ClienteRequest request) {
        if (clienteRepository.existsByRestauranteIdAndTelefone(restauranteId, request.getTelefone())) {
            throw new BusinessException("Já existe um cliente cadastrado com o telefone '" + request.getTelefone() + "'");
        }
        Cliente cliente = clienteRepository.save(preencher(Cliente.builder()
            .restauranteId(restauranteId)
            .ativo(true)
            .build(), request));
        return toResponse(cliente);
    }

    public ClienteResponse atualizar(Long restauranteId, Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(restauranteId, id);
        if (!cliente.getTelefone().equals(request.getTelefone())
                && clienteRepository.existsByRestauranteIdAndTelefone(restauranteId, request.getTelefone())) {
            throw new BusinessException("Já existe um cliente cadastrado com o telefone '" + request.getTelefone() + "'");
        }
        return toResponse(clienteRepository.save(preencher(cliente, request)));
    }

    public ClienteResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Cliente cliente = buscarEntidade(restauranteId, id);
        cliente.setAtivo(ativo);
        return toResponse(clienteRepository.save(cliente));
    }

    Cliente buscarEntidade(Long restauranteId, Long id) {
        return clienteRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }

    private Cliente preencher(Cliente cliente, ClienteRequest request) {
        cliente.setNome(request.getNome());
        cliente.setTelefone(request.getTelefone());
        cliente.setEmail(request.getEmail());
        cliente.setDataNascimento(request.getDataNascimento());
        cliente.setEnderecoRua(request.getEnderecoRua());
        cliente.setEnderecoNumero(request.getEnderecoNumero());
        cliente.setEnderecoBairro(request.getEnderecoBairro());
        cliente.setEnderecoCidade(request.getEnderecoCidade());
        cliente.setEnderecoComplemento(request.getEnderecoComplemento());
        cliente.setEnderecoCep(request.getEnderecoCep());
        cliente.setObservacoes(request.getObservacoes());
        return cliente;
    }

    private ClienteResponse toResponse(Cliente c) {
        return ClienteResponse.builder()
            .id(c.getId()).restauranteId(c.getRestauranteId())
            .nome(c.getNome()).telefone(c.getTelefone()).email(c.getEmail())
            .dataNascimento(c.getDataNascimento())
            .enderecoRua(c.getEnderecoRua()).enderecoNumero(c.getEnderecoNumero())
            .enderecoBairro(c.getEnderecoBairro()).enderecoCidade(c.getEnderecoCidade())
            .enderecoComplemento(c.getEnderecoComplemento()).enderecoCep(c.getEnderecoCep())
            .observacoes(c.getObservacoes()).ativo(c.isAtivo()).createdAt(c.getCreatedAt())
            .build();
    }
}
