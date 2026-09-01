package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.ComboGrupoRequest;
import com.oiaaconta.catalog.dto.request.ComboRequest;
import com.oiaaconta.catalog.dto.response.ComboGrupoProdutoResponse;
import com.oiaaconta.catalog.dto.response.ComboGrupoResponse;
import com.oiaaconta.catalog.dto.response.ComboResponse;
import com.oiaaconta.catalog.entity.Combo;
import com.oiaaconta.catalog.entity.ComboGrupo;
import com.oiaaconta.catalog.entity.ComboGrupoProduto;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.ComboGrupoProdutoRepository;
import com.oiaaconta.catalog.repository.ComboGrupoRepository;
import com.oiaaconta.catalog.repository.ComboRepository;
import com.oiaaconta.catalog.repository.ProdutoRepository;
import com.oiaaconta.catalog.util.ImagemValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComboService {

    private static final int IMAGEM_MAX_CHARS = 1_400_000;

    private final ComboRepository comboRepository;
    private final ComboGrupoRepository comboGrupoRepository;
    private final ComboGrupoProdutoRepository comboGrupoProdutoRepository;
    private final ProdutoRepository produtoRepository;

    public List<ComboResponse> listar(Long restauranteId, boolean apenasAtivos) {
        List<Combo> combos = apenasAtivos
            ? comboRepository.findByRestauranteIdAndAtivoTrueOrderByNomeAsc(restauranteId)
            : comboRepository.findByRestauranteIdOrderByNomeAsc(restauranteId);
        return combos.stream().map(c -> toResponse(c, restauranteId)).toList();
    }

    public ComboResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(buscarEntidade(restauranteId, id), restauranteId);
    }

    @Transactional
    public ComboResponse criar(Long restauranteId, ComboRequest request) {
        validarGrupos(restauranteId, request.getGrupos());
        Combo combo = comboRepository.save(Combo.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome()).descricao(request.getDescricao())
            .preco(request.getPreco())
            .imagemBase64(validarImagemOuLimpar(request.getImagemBase64(), null))
            .numeroCardapio(request.getNumeroCardapio())
            .ativo(true)
            .build());
        salvarGrupos(combo.getId(), request.getGrupos());
        return toResponse(combo, restauranteId);
    }

    @Transactional
    public ComboResponse atualizar(Long restauranteId, Long id, ComboRequest request) {
        Combo combo = buscarEntidade(restauranteId, id);
        validarGrupos(restauranteId, request.getGrupos());
        combo.setNome(request.getNome());
        combo.setDescricao(request.getDescricao());
        combo.setPreco(request.getPreco());
        combo.setImagemBase64(validarImagemOuLimpar(request.getImagemBase64(), combo.getImagemBase64()));
        combo.setNumeroCardapio(request.getNumeroCardapio());
        comboRepository.save(combo);
        comboGrupoRepository.deleteByComboId(id);
        // ComboGrupo/ComboGrupoProduto usam IDENTITY — sem flush aqui, o
        // INSERT dos grupos novos roda antes do DELETE ter efeito no banco
        // (mesma causa do bug corrigido em combo_itens).
        comboGrupoRepository.flush();
        salvarGrupos(id, request.getGrupos());
        return toResponse(combo, restauranteId);
    }

    public ComboResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Combo combo = buscarEntidade(restauranteId, id);
        combo.setAtivo(ativo);
        return toResponse(comboRepository.save(combo), restauranteId);
    }

    private void validarGrupos(Long restauranteId, List<ComboGrupoRequest> grupos) {
        for (ComboGrupoRequest grupo : grupos) {
            for (Long produtoId : grupo.getProdutoIds()) {
                produtoRepository.findByIdAndRestauranteId(produtoId, restauranteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto #" + produtoId + " não encontrado"));
            }
        }
    }

    private void salvarGrupos(Long comboId, List<ComboGrupoRequest> grupos) {
        int ordem = 0;
        for (ComboGrupoRequest grupoReq : grupos) {
            ComboGrupo grupo = comboGrupoRepository.save(ComboGrupo.builder()
                .comboId(comboId).nome(grupoReq.getNome())
                .quantidade(grupoReq.getQuantidade()).ordem(ordem++)
                .build());
            for (Long produtoId : grupoReq.getProdutoIds()) {
                comboGrupoProdutoRepository.save(ComboGrupoProduto.builder()
                    .grupoId(grupo.getId()).produtoId(produtoId)
                    .build());
            }
        }
    }

    Combo buscarEntidade(Long restauranteId, Long id) {
        return comboRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Combo não encontrado"));
    }

    private ComboResponse toResponse(Combo combo, Long restauranteId) {
        List<ComboGrupo> grupos = comboGrupoRepository.findByComboIdOrderByOrdemAsc(combo.getId());
        List<Long> grupoIds = grupos.stream().map(ComboGrupo::getId).toList();
        Map<Long, List<ComboGrupoProduto>> produtosPorGrupo = new LinkedHashMap<>();
        if (!grupoIds.isEmpty()) {
            for (ComboGrupoProduto gp : comboGrupoProdutoRepository.findByGrupoIdIn(grupoIds)) {
                produtosPorGrupo.computeIfAbsent(gp.getGrupoId(), k -> new java.util.ArrayList<>()).add(gp);
            }
        }

        List<ComboGrupoResponse> gruposResponse = grupos.stream().map(g -> {
            List<ComboGrupoProdutoResponse> produtos = produtosPorGrupo.getOrDefault(g.getId(), List.of()).stream()
                .map(gp -> produtoRepository.findByIdAndRestauranteId(gp.getProdutoId(), restauranteId)
                    .map(p -> ComboGrupoProdutoResponse.builder()
                        .produtoId(p.getId()).nome(p.getNome()).preco(p.getPreco())
                        .build())
                    .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
            return ComboGrupoResponse.builder()
                .id(g.getId()).nome(g.getNome()).quantidade(g.getQuantidade())
                .produtos(produtos)
                .build();
        }).toList();

        return ComboResponse.builder()
            .id(combo.getId()).restauranteId(combo.getRestauranteId())
            .nome(combo.getNome()).descricao(combo.getDescricao())
            .preco(combo.getPreco()).imagemBase64(combo.getImagemBase64())
            .numeroCardapio(combo.getNumeroCardapio())
            .ativo(combo.isAtivo()).grupos(gruposResponse)
            .build();
    }

    // Cardápio numerado (chatbot WhatsApp) — mesmo formato usado por
    // ProdutoService.listarNumerados, unidos pelo PublicoCatalogController
    // num único espaço de numeração (produtoId null = é combo).
    public List<com.oiaaconta.catalog.dto.response.ProdutoNumeradoResponse> listarNumerados(Long restauranteId) {
        return comboRepository.findByRestauranteIdAndAtivoTrueAndNumeroCardapioIsNotNullOrderByNumeroCardapioAsc(restauranteId)
            .stream()
            .map(c -> com.oiaaconta.catalog.dto.response.ProdutoNumeradoResponse.builder()
                .numero(c.getNumeroCardapio())
                .comboId(c.getId())
                .nome(c.getNome())
                .preco(c.getPreco())
                .build())
            .toList();
    }

    private String validarImagemOuLimpar(String novaImagem, String imagemAtual) {
        if (novaImagem == null) return imagemAtual;
        if (novaImagem.isBlank()) return null;
        ImagemValidator.validar(novaImagem);
        if (novaImagem.length() > IMAGEM_MAX_CHARS) {
            throw new BusinessException("Imagem muito grande. Envie um arquivo menor (até ~1MB).");
        }
        return novaImagem;
    }
}
