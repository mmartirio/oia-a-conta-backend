package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.CupomRequest;
import com.oiaaconta.catalog.dto.response.CupomResponse;
import com.oiaaconta.catalog.dto.response.CupomValidacaoResponse;
import com.oiaaconta.catalog.entity.Cliente;
import com.oiaaconta.catalog.entity.Cupom;
import com.oiaaconta.catalog.entity.GrupoCliente;
import com.oiaaconta.catalog.enums.TipoAlvo;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.ClienteRepository;
import com.oiaaconta.catalog.repository.CupomRepository;
import com.oiaaconta.catalog.repository.GrupoClienteMembroRepository;
import com.oiaaconta.catalog.repository.GrupoClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CupomService {

    private final CupomRepository cupomRepository;
    private final GrupoClienteRepository grupoClienteRepository;
    private final GrupoClienteMembroRepository grupoClienteMembroRepository;
    private final ClienteRepository clienteRepository;

    public List<CupomResponse> listar(Long restauranteId) {
        return cupomRepository.findByRestauranteIdOrderByCreatedAtDesc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public CupomResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(buscarEntidade(restauranteId, id));
    }

    public CupomResponse criar(Long restauranteId, CupomRequest request) {
        String codigo = normalizarCodigo(request.getCodigo());
        if (cupomRepository.existsByRestauranteIdAndCodigo(restauranteId, codigo)) {
            throw new BusinessException("Já existe um cupom com o código '" + codigo + "'");
        }
        validarAlvo(restauranteId, request);
        validarPeriodo(request.getValidoDe(), request.getValidoAte());
        Cupom cupom = cupomRepository.save(Cupom.builder()
            .restauranteId(restauranteId)
            .codigo(codigo)
            .tipoDesconto(request.getTipoDesconto()).valorDesconto(request.getValorDesconto())
            .tipoAlvo(request.getTipoAlvo())
            .grupoClienteId(request.getTipoAlvo() == TipoAlvo.GRUPO ? request.getGrupoClienteId() : null)
            .clienteId(request.getTipoAlvo() == TipoAlvo.INDIVIDUAL ? request.getClienteId() : null)
            .validoDe(request.getValidoDe()).validoAte(request.getValidoAte())
            .ativo(true)
            .build());
        return toResponse(cupom);
    }

    public CupomResponse atualizar(Long restauranteId, Long id, CupomRequest request) {
        Cupom cupom = buscarEntidade(restauranteId, id);
        String codigo = normalizarCodigo(request.getCodigo());
        if (!cupom.getCodigo().equals(codigo) && cupomRepository.existsByRestauranteIdAndCodigo(restauranteId, codigo)) {
            throw new BusinessException("Já existe um cupom com o código '" + codigo + "'");
        }
        validarAlvo(restauranteId, request);
        validarPeriodo(request.getValidoDe(), request.getValidoAte());
        cupom.setCodigo(codigo);
        cupom.setTipoDesconto(request.getTipoDesconto());
        cupom.setValorDesconto(request.getValorDesconto());
        cupom.setTipoAlvo(request.getTipoAlvo());
        cupom.setGrupoClienteId(request.getTipoAlvo() == TipoAlvo.GRUPO ? request.getGrupoClienteId() : null);
        cupom.setClienteId(request.getTipoAlvo() == TipoAlvo.INDIVIDUAL ? request.getClienteId() : null);
        cupom.setValidoDe(request.getValidoDe());
        cupom.setValidoAte(request.getValidoAte());
        return toResponse(cupomRepository.save(cupom));
    }

    public CupomResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Cupom cupom = buscarEntidade(restauranteId, id);
        cupom.setAtivo(ativo);
        return toResponse(cupomRepository.save(cupom));
    }

    public CupomValidacaoResponse validar(Long restauranteId, String codigo, Long clienteId) {
        Cupom cupom = cupomRepository.findByRestauranteIdAndCodigo(restauranteId, normalizarCodigo(codigo)).orElse(null);
        if (cupom == null) {
            return invalido("Cupom não encontrado");
        }
        if (!cupom.isAtivo()) {
            return invalido("Cupom inativo");
        }
        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(cupom.getValidoDe()) || hoje.isAfter(cupom.getValidoAte())) {
            return invalido("Cupom fora do período de validade");
        }
        if (cupom.getTipoAlvo() == TipoAlvo.INDIVIDUAL) {
            if (clienteId == null || !clienteId.equals(cupom.getClienteId())) {
                return invalido("Cupom não é válido para este cliente");
            }
        } else if (cupom.getTipoAlvo() == TipoAlvo.GRUPO) {
            if (clienteId == null || !grupoClienteMembroRepository.existsByGrupoClienteIdAndClienteId(cupom.getGrupoClienteId(), clienteId)) {
                return invalido("Cliente não pertence ao grupo elegível para este cupom");
            }
        }
        return CupomValidacaoResponse.builder()
            .valido(true).cupomId(cupom.getId()).codigo(cupom.getCodigo())
            .tipoDesconto(cupom.getTipoDesconto()).valorDesconto(cupom.getValorDesconto())
            .build();
    }

    private CupomValidacaoResponse invalido(String motivo) {
        return CupomValidacaoResponse.builder().valido(false).motivoInvalido(motivo).build();
    }

    private void validarAlvo(Long restauranteId, CupomRequest request) {
        if (request.getTipoAlvo() == TipoAlvo.GRUPO) {
            if (request.getGrupoClienteId() == null) {
                throw new BusinessException("Selecione o grupo de clientes para este cupom");
            }
            grupoClienteRepository.findByIdAndRestauranteId(request.getGrupoClienteId(), restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de clientes não encontrado"));
        } else if (request.getTipoAlvo() == TipoAlvo.INDIVIDUAL) {
            if (request.getClienteId() == null) {
                throw new BusinessException("Selecione o cliente para este cupom");
            }
            clienteRepository.findByIdAndRestauranteId(request.getClienteId(), restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        }
    }

    private void validarPeriodo(LocalDate validoDe, LocalDate validoAte) {
        if (validoAte.isBefore(validoDe)) {
            throw new BusinessException("Data de fim da validade não pode ser anterior à data de início");
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase();
    }

    Cupom buscarEntidade(Long restauranteId, Long id) {
        return cupomRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado"));
    }

    private CupomResponse toResponse(Cupom c) {
        String grupoNome = null;
        if (c.getGrupoClienteId() != null) {
            grupoNome = grupoClienteRepository.findByIdAndRestauranteId(c.getGrupoClienteId(), c.getRestauranteId())
                .map(GrupoCliente::getNome).orElse(null);
        }
        String clienteNome = null;
        if (c.getClienteId() != null) {
            clienteNome = clienteRepository.findByIdAndRestauranteId(c.getClienteId(), c.getRestauranteId())
                .map(Cliente::getNome).orElse(null);
        }
        return CupomResponse.builder()
            .id(c.getId()).restauranteId(c.getRestauranteId()).codigo(c.getCodigo())
            .tipoDesconto(c.getTipoDesconto()).valorDesconto(c.getValorDesconto())
            .tipoAlvo(c.getTipoAlvo())
            .grupoClienteId(c.getGrupoClienteId()).grupoClienteNome(grupoNome)
            .clienteId(c.getClienteId()).clienteNome(clienteNome)
            .validoDe(c.getValidoDe()).validoAte(c.getValidoAte()).ativo(c.isAtivo())
            .build();
    }
}
