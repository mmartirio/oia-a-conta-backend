package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.CategoriaRequest;
import com.oiaaconta.catalog.dto.response.CategoriaResponse;
import com.oiaaconta.catalog.entity.Categoria;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<CategoriaResponse> listar(Long restauranteId) {
        return categoriaRepository.findByRestauranteIdAndAtivoTrueOrderByNomeAsc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    @SuppressWarnings("null")
    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public CategoriaResponse criar(Long restauranteId, CategoriaRequest request) {
        if (categoriaRepository.existsByRestauranteIdAndNome(restauranteId, request.getNome())) {
            throw new BusinessException("Categoria '" + request.getNome() + "' já existe");
        }
        Categoria categoria = categoriaRepository.save(Categoria.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome())
            .ativo(true)
            .build());
        return toResponse(categoria);
    }

    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public CategoriaResponse atualizar(Long restauranteId, Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        categoria.setNome(request.getNome());
        return toResponse(categoriaRepository.save(categoria));
    }

    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public void desativar(Long restauranteId, Long id) {
        Categoria categoria = categoriaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }

    private CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.builder()
            .id(c.getId()).restauranteId(c.getRestauranteId())
            .nome(c.getNome()).ativo(c.isAtivo()).build();
    }
}
