package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.client.IfoodSyncClient;
import com.oiaaconta.catalog.dto.request.ProdutoRequest;
import com.oiaaconta.catalog.dto.response.ProdutoResponse;
import com.oiaaconta.catalog.entity.Produto;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.CategoriaRepository;
import com.oiaaconta.catalog.repository.ProdutoRepository;
import com.oiaaconta.catalog.util.ImagemValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {

    // ~1MB de imagem original vira ~1,4M de caracteres em base64 — teto para a foto do produto.
    private static final int IMAGEM_MAX_CHARS = 1_400_000;

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final IfoodSyncClient ifoodSyncClient;

    public List<ProdutoResponse> listar(Long restauranteId, boolean incluirInativos) {
        List<Produto> produtos = incluirInativos
            ? produtoRepository.findByRestauranteIdOrderByNomeAsc(restauranteId)
            : produtoRepository.findByRestauranteIdAndAtivoTrueOrderByNomeAsc(restauranteId);
        return produtos.stream().map(p -> toResponse(p, restauranteId)).toList();
    }

    public List<ProdutoResponse> listarPorCategoria(Long restauranteId, Long categoriaId) {
        return produtoRepository.findByCategoriaIdAndRestauranteIdAndAtivoTrue(categoriaId, restauranteId)
            .stream().map(p -> toResponse(p, restauranteId)).toList();
    }

    @SuppressWarnings("null")
    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public ProdutoResponse criar(Long restauranteId, ProdutoRequest request) {
        categoriaRepository.findByIdAndRestauranteId(request.getCategoriaId(), restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));

        Produto produto = produtoRepository.save(Produto.builder()
            .restauranteId(restauranteId)
            .categoriaId(request.getCategoriaId())
            .nome(request.getNome())
            .descricao(request.getDescricao())
            .preco(request.getPreco())
            .imagemBase64(validarImagemOuLimpar(request.getImagemBase64(), null))
            .numeroCardapio(request.getNumeroCardapio())
            .ativo(true)
            .build());
        notificarIfood(restauranteId);
        return toResponse(produto, restauranteId);
    }

    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public ProdutoResponse atualizar(Long restauranteId, Long id, ProdutoRequest request) {
        Produto produto = produtoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        produto.setNome(request.getNome());
        produto.setDescricao(request.getDescricao());
        produto.setPreco(request.getPreco());
        produto.setCategoriaId(request.getCategoriaId());
        produto.setNumeroCardapio(request.getNumeroCardapio());
        // null = campo ausente, não mexe na imagem atual; "" explícito = remove a imagem.
        produto.setImagemBase64(validarImagemOuLimpar(request.getImagemBase64(), produto.getImagemBase64()));
        ProdutoResponse response = toResponse(produtoRepository.save(produto), restauranteId);
        notificarIfood(restauranteId);
        return response;
    }

    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public void desativar(Long restauranteId, Long id) {
        alterarAtivo(restauranteId, id, false);
    }

    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public ProdutoResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Produto produto = produtoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        produto.setAtivo(ativo);
        ProdutoResponse response = toResponse(produtoRepository.save(produto), restauranteId);
        notificarIfood(restauranteId);
        return response;
    }

    // Adia pra depois do commit quando chamado de dentro de uma transação
    // (evita o ifood-service reler o cardápio, via HTTP/outra conexão,
    // antes da mudança estar commitada — ver ComboService, que tem o
    // mesmo helper por causa disso). Nenhum método aqui é @Transactional
    // hoje, mas o helper fica seguro pra isso mudar sem reintroduzir o bug.
    private void notificarIfood(Long restauranteId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispararSyncIfood(restauranteId);
                }
            });
        } else {
            dispararSyncIfood(restauranteId);
        }
    }

    // Best-effort — nunca deve travar o CRUD local por uma falha na
    // integração externa. É um no-op silencioso pra quem não tem iFood
    // vinculado (ver IfoodCatalogSyncService.sincronizarSeVinculado).
    private void dispararSyncIfood(Long restauranteId) {
        try {
            ifoodSyncClient.sincronizarCatalogo(restauranteId);
        } catch (Exception e) {
            log.warn("Falha ao notificar iFood sobre mudança de produto (restaurante {}): {}", restauranteId, e.getMessage());
        }
    }

    private ProdutoResponse toResponse(Produto p, Long restauranteId) {
        String categoriaNome = categoriaRepository.findByIdAndRestauranteId(p.getCategoriaId(), restauranteId)
            .map(c -> c.getNome()).orElse(null);
        return ProdutoResponse.builder()
            .id(p.getId()).restauranteId(p.getRestauranteId())
            .categoriaId(p.getCategoriaId()).categoriaNome(categoriaNome)
            .nome(p.getNome()).descricao(p.getDescricao())
            .preco(p.getPreco()).imagemBase64(p.getImagemBase64())
            .numeroCardapio(p.getNumeroCardapio())
            .ativo(p.isAtivo()).build();
    }

    // Cardápio numerado (chatbot WhatsApp) — não passa por toResponse porque é
    // consumido por whatsapp-service via um DTO próprio, mais enxuto.
    public List<com.oiaaconta.catalog.dto.response.ProdutoNumeradoResponse> listarNumerados(Long restauranteId) {
        return produtoRepository.findByRestauranteIdAndAtivoTrueAndNumeroCardapioIsNotNullOrderByNumeroCardapioAsc(restauranteId)
            .stream()
            .map(p -> com.oiaaconta.catalog.dto.response.ProdutoNumeradoResponse.builder()
                .numero(p.getNumeroCardapio())
                .produtoId(p.getId())
                .nome(p.getNome())
                .preco(p.getPreco())
                .build())
            .toList();
    }

    // null = não altera (mantém a imagem atual); "" explícito = remove; caso contrário valida e usa a nova.
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
