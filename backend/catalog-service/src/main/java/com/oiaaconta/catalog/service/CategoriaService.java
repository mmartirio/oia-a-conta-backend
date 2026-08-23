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

    // Nome da categoria que sempre aparece por último no cardápio, não
    // importa a posição gravada em "ordem" — pedido do negócio: bebida é
    // sempre a última coisa listada, pra não competir com os pratos.
    private static final String CATEGORIA_SEMPRE_POR_ULTIMO = "Bebidas";

    public List<CategoriaResponse> listar(Long restauranteId, boolean incluirInativos) {
        List<Categoria> categorias = incluirInativos
            ? categoriaRepository.findByRestauranteIdOrderByOrdemAscNomeAsc(restauranteId)
            : categoriaRepository.findByRestauranteIdAndAtivoTrueOrderByOrdemAscNomeAsc(restauranteId);
        return categorias.stream()
            .sorted(java.util.Comparator.comparing(c -> CATEGORIA_SEMPRE_POR_ULTIMO.equalsIgnoreCase(c.getNome())))
            .map(this::toResponse)
            .toList();
    }

    // Chamado no cadastro de um novo restaurante (via /internal/categorias/padrao,
    // pelo auth-service) — dá um ponto de partida pronto de categorias, sem
    // impedir a criação de outras (mesmo espírito do GrupoService.criarGruposPadrao).
    // Não faz nada se o restaurante já tiver alguma categoria (evita duplicar
    // em reprocessamentos/retries da chamada best-effort do auth-service).
    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public void criarCategoriasPadrao(Long restauranteId) {
        if (!categoriaRepository.findByRestauranteIdOrderByOrdemAscNomeAsc(restauranteId).isEmpty()) {
            return;
        }
        List<String> nomes = List.of("Entradas", "Pratos Principais", "Sobremesas", "Bebidas");
        for (int i = 0; i < nomes.size(); i++) {
            categoriaRepository.save(Categoria.builder()
                .restauranteId(restauranteId)
                .nome(nomes.get(i))
                .ativo(true)
                .ordem(i)
                .build());
        }
    }

    @SuppressWarnings("null")
    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public CategoriaResponse criar(Long restauranteId, CategoriaRequest request) {
        if (categoriaRepository.existsByRestauranteIdAndNome(restauranteId, request.getNome())) {
            throw new BusinessException("Categoria '" + request.getNome() + "' já existe");
        }
        // Categoria nova entra no fim da lista, não embaralha o que já existe.
        int ordem = categoriaRepository.maiorOrdem(restauranteId) + 1;
        Categoria categoria = categoriaRepository.save(Categoria.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome())
            .ativo(true)
            .ordem(ordem)
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
        alterarAtivo(restauranteId, id, false);
    }

    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public CategoriaResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Categoria categoria = categoriaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        categoria.setAtivo(ativo);
        return toResponse(categoriaRepository.save(categoria));
    }

    // Recebe a lista completa de IDs de categoria na ordem desejada e grava
    // a posição (0, 1, 2...) de cada uma. Precisa vir com todas as
    // categorias do restaurante — categorias existentes que não estiverem
    // na lista mantêm sua ordem atual (não são tocadas).
    @CacheEvict(value = "cardapio-publico", key = "#restauranteId")
    public List<CategoriaResponse> reordenar(Long restauranteId, List<Long> idsNaNovaOrdem) {
        List<Categoria> categorias = categoriaRepository.findByRestauranteIdAndIdIn(restauranteId, idsNaNovaOrdem);
        if (categorias.size() != idsNaNovaOrdem.size()) {
            throw new BusinessException("Uma ou mais categorias não pertencem a este restaurante");
        }
        java.util.Map<Long, Categoria> porId = categorias.stream()
            .collect(java.util.stream.Collectors.toMap(Categoria::getId, c -> c));
        for (int i = 0; i < idsNaNovaOrdem.size(); i++) {
            porId.get(idsNaNovaOrdem.get(i)).setOrdem(i);
        }
        categoriaRepository.saveAll(categorias);
        return listar(restauranteId, true);
    }

    private CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.builder()
            .id(c.getId()).restauranteId(c.getRestauranteId())
            .nome(c.getNome()).ativo(c.isAtivo()).ordem(c.getOrdem()).build();
    }
}
