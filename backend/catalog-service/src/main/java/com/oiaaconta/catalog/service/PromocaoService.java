package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.PromocaoRequest;
import com.oiaaconta.catalog.dto.response.PromocaoAplicavelResponse;
import com.oiaaconta.catalog.dto.response.PromocaoResponse;
import com.oiaaconta.catalog.entity.GrupoCliente;
import com.oiaaconta.catalog.entity.Promocao;
import com.oiaaconta.catalog.enums.TipoAlvo;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.GrupoClienteMembroRepository;
import com.oiaaconta.catalog.repository.GrupoClienteRepository;
import com.oiaaconta.catalog.repository.PromocaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromocaoService {

    private final PromocaoRepository promocaoRepository;
    private final GrupoClienteRepository grupoClienteRepository;
    private final GrupoClienteMembroRepository grupoClienteMembroRepository;

    public List<PromocaoResponse> listar(Long restauranteId) {
        return promocaoRepository.findByRestauranteIdOrderByCreatedAtDesc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public PromocaoResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(buscarEntidade(restauranteId, id));
    }

    public PromocaoResponse criar(Long restauranteId, PromocaoRequest request) {
        validar(restauranteId, request);
        Promocao promocao = promocaoRepository.save(Promocao.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome()).descricao(request.getDescricao())
            .tipoDesconto(request.getTipoDesconto()).valorDesconto(request.getValorDesconto())
            .tipoAlvo(request.getTipoAlvo())
            .grupoClienteId(request.getTipoAlvo() == TipoAlvo.GRUPO ? request.getGrupoClienteId() : null)
            .requisitoGastoMinimo(request.getRequisitoGastoMinimo())
            .validoDe(request.getValidoDe()).validoAte(request.getValidoAte())
            .ativo(true)
            .build());
        return toResponse(promocao);
    }

    public PromocaoResponse atualizar(Long restauranteId, Long id, PromocaoRequest request) {
        Promocao promocao = buscarEntidade(restauranteId, id);
        validar(restauranteId, request);
        promocao.setNome(request.getNome());
        promocao.setDescricao(request.getDescricao());
        promocao.setTipoDesconto(request.getTipoDesconto());
        promocao.setValorDesconto(request.getValorDesconto());
        promocao.setTipoAlvo(request.getTipoAlvo());
        promocao.setGrupoClienteId(request.getTipoAlvo() == TipoAlvo.GRUPO ? request.getGrupoClienteId() : null);
        promocao.setRequisitoGastoMinimo(request.getRequisitoGastoMinimo());
        promocao.setValidoDe(request.getValidoDe());
        promocao.setValidoAte(request.getValidoAte());
        return toResponse(promocaoRepository.save(promocao));
    }

    public PromocaoResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Promocao promocao = buscarEntidade(restauranteId, id);
        promocao.setAtivo(ativo);
        return toResponse(promocaoRepository.save(promocao));
    }

    // Promoções ativas, dentro da validade, cujo alvo é TODOS ou GRUPO (cliente
    // é membro) e cujo requisito de gasto mínimo (se houver) é atendido pelo
    // gastoHistorico informado pelo order-service (catalog não tem esse dado).
    public List<PromocaoAplicavelResponse> aplicaveis(Long restauranteId, Long clienteId, BigDecimal gastoHistorico) {
        LocalDate hoje = LocalDate.now();
        BigDecimal gasto = gastoHistorico != null ? gastoHistorico : BigDecimal.ZERO;
        return promocaoRepository.findByRestauranteIdAndAtivoTrue(restauranteId).stream()
            .filter(p -> !hoje.isBefore(p.getValidoDe()) && !hoje.isAfter(p.getValidoAte()))
            .filter(p -> alvoElegivel(p, clienteId))
            .filter(p -> p.getRequisitoGastoMinimo() == null || gasto.compareTo(p.getRequisitoGastoMinimo()) >= 0)
            .map(p -> PromocaoAplicavelResponse.builder()
                .promocaoId(p.getId()).nome(p.getNome())
                .tipoDesconto(p.getTipoDesconto()).valorDesconto(p.getValorDesconto())
                .build())
            .toList();
    }

    private boolean alvoElegivel(Promocao p, Long clienteId) {
        if (p.getTipoAlvo() == TipoAlvo.TODOS) return true;
        return clienteId != null && grupoClienteMembroRepository.existsByGrupoClienteIdAndClienteId(p.getGrupoClienteId(), clienteId);
    }

    private void validar(Long restauranteId, PromocaoRequest request) {
        if (request.getTipoAlvo() == TipoAlvo.INDIVIDUAL) {
            throw new BusinessException("Promoção não pode ter alvo individual — use um Cupom para isso");
        }
        if (request.getTipoAlvo() == TipoAlvo.GRUPO) {
            if (request.getGrupoClienteId() == null) {
                throw new BusinessException("Selecione o grupo de clientes para esta promoção");
            }
            grupoClienteRepository.findByIdAndRestauranteId(request.getGrupoClienteId(), restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de clientes não encontrado"));
        }
        if (request.getValidoAte().isBefore(request.getValidoDe())) {
            throw new BusinessException("Data de fim da validade não pode ser anterior à data de início");
        }
    }

    Promocao buscarEntidade(Long restauranteId, Long id) {
        return promocaoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Promoção não encontrada"));
    }

    private PromocaoResponse toResponse(Promocao p) {
        String grupoNome = null;
        if (p.getGrupoClienteId() != null) {
            grupoNome = grupoClienteRepository.findByIdAndRestauranteId(p.getGrupoClienteId(), p.getRestauranteId())
                .map(GrupoCliente::getNome).orElse(null);
        }
        return PromocaoResponse.builder()
            .id(p.getId()).restauranteId(p.getRestauranteId())
            .nome(p.getNome()).descricao(p.getDescricao())
            .tipoDesconto(p.getTipoDesconto()).valorDesconto(p.getValorDesconto())
            .tipoAlvo(p.getTipoAlvo())
            .grupoClienteId(p.getGrupoClienteId()).grupoClienteNome(grupoNome)
            .requisitoGastoMinimo(p.getRequisitoGastoMinimo())
            .validoDe(p.getValidoDe()).validoAte(p.getValidoAte()).ativo(p.isAtivo())
            .build();
    }
}
