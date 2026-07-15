package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.AbrirCaixaRequest;
import com.oiaaconta.order.dto.request.FecharCaixaRequest;
import com.oiaaconta.order.dto.response.SessaoCaixaResponse;
import com.oiaaconta.order.entity.SessaoCaixa;
import com.oiaaconta.order.enums.StatusSessaoCaixa;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.repository.SessaoCaixaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaixaService {

    private final SessaoCaixaRepository sessaoCaixaRepository;

    public SessaoCaixaResponse status(Long restauranteId) {
        return sessaoCaixaRepository
            .findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .map(this::toResponse)
            .orElse(null);
    }

    public List<SessaoCaixaResponse> historico(Long restauranteId) {
        return sessaoCaixaRepository.findByRestauranteIdOrderByAbertoEmDesc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SessaoCaixaResponse abrir(Long restauranteId, Long usuarioId, String usuarioNome, AbrirCaixaRequest request) {
        sessaoCaixaRepository.findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .ifPresent(s -> { throw new BusinessException("Já existe um caixa aberto"); });

        SessaoCaixa sessao = sessaoCaixaRepository.save(SessaoCaixa.builder()
            .restauranteId(restauranteId)
            .abertoPorId(usuarioId)
            .abertoPorNome(usuarioNome)
            .valorAbertura(request.getValorAbertura())
            .status(StatusSessaoCaixa.ABERTO)
            .build());
        return toResponse(sessao);
    }

    @Transactional
    public SessaoCaixaResponse fechar(Long restauranteId, Long usuarioId, String usuarioNome, FecharCaixaRequest request) {
        SessaoCaixa sessao = sessaoCaixaRepository
            .findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(restauranteId, StatusSessaoCaixa.ABERTO)
            .orElseThrow(() -> new BusinessException("Não há caixa aberto"));

        sessao.setStatus(StatusSessaoCaixa.FECHADO);
        sessao.setValorFechamento(request.getValorFechamento());
        sessao.setFechadoPorId(usuarioId);
        sessao.setFechadoPorNome(usuarioNome);
        sessao.setFechadoEm(java.time.LocalDateTime.now());
        return toResponse(sessaoCaixaRepository.save(sessao));
    }

    private SessaoCaixaResponse toResponse(SessaoCaixa s) {
        return SessaoCaixaResponse.builder()
            .id(s.getId())
            .abertoPorNome(s.getAbertoPorNome())
            .valorAbertura(s.getValorAbertura())
            .abertoEm(s.getAbertoEm())
            .fechadoPorNome(s.getFechadoPorNome())
            .valorFechamento(s.getValorFechamento())
            .fechadoEm(s.getFechadoEm())
            .status(s.getStatus())
            .build();
    }
}
