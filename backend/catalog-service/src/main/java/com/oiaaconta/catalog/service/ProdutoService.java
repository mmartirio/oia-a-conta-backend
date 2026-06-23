package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.ProdutoRequest;
import com.oiaaconta.catalog.dto.response.ProdutoResponse;
import com.oiaaconta.catalog.entity.Produto;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.CategoriaRepository;
import com.oiaaconta.catalog.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public List<ProdutoResponse> listar(Long restauranteId) {
        return produtoRepository.findByRestauranteIdAndAtivoTrueOrderByNomeAsc(restauranteId)
            .stream().map(p -> toResponse(p, restauranteId)).toList();
    }

    public List<ProdutoResponse> listarPorCategoria(Long restauranteId, Long categoriaId) {
        return produtoRepository.findByCategoriaIdAndRestauranteIdAndAtivoTrue(categoriaId, restauranteId)
            .stream().map(p -> toResponse(p, restauranteId)).toList();
    }

    @SuppressWarnings("null")
    public ProdutoResponse criar(Long restauranteId, ProdutoRequest request) {
        categoriaRepository.findByIdAndRestauranteId(request.getCategoriaId(), restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));

        Produto produto = produtoRepository.save(Produto.builder()
            .restauranteId(restauranteId)
            .categoriaId(request.getCategoriaId())
            .nome(request.getNome())
            .descricao(request.getDescricao())
            .preco(request.getPreco())
            .ativo(true)
            .build());
        return toResponse(produto, restauranteId);
    }

    public ProdutoResponse atualizar(Long restauranteId, Long id, ProdutoRequest request) {
        Produto produto = produtoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        produto.setNome(request.getNome());
        produto.setDescricao(request.getDescricao());
        produto.setPreco(request.getPreco());
        produto.setCategoriaId(request.getCategoriaId());
        return toResponse(produtoRepository.save(produto), restauranteId);
    }

    public void desativar(Long restauranteId, Long id) {
        Produto produto = produtoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    private ProdutoResponse toResponse(Produto p, Long restauranteId) {
        String categoriaNome = categoriaRepository.findByIdAndRestauranteId(p.getCategoriaId(), restauranteId)
            .map(c -> c.getNome()).orElse(null);
        return ProdutoResponse.builder()
            .id(p.getId()).restauranteId(p.getRestauranteId())
            .categoriaId(p.getCategoriaId()).categoriaNome(categoriaNome)
            .nome(p.getNome()).descricao(p.getDescricao())
            .preco(p.getPreco()).ativo(p.isAtivo()).build();
    }
}
