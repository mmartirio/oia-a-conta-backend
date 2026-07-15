package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.response.CardapioCategoriaResponse;
import com.oiaaconta.catalog.dto.response.CategoriaResponse;
import com.oiaaconta.catalog.dto.response.ProdutoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Monta o cardápio público (categorias ativas + produtos ativos de cada uma).
 *
 * NÃO cacheado (removido de propósito): o valor retornado é uma
 * List&lt;CardapioCategoriaResponse&gt; com List&lt;ProdutoResponse&gt; aninhada, e o
 * GenericJackson2JsonRedisSerializer (tipagem polimórfica automática do
 * Jackson) se mostrou incompatível com esse formato em produção — a escrita no
 * cache funcionava, mas a leitura de volta falhava
 * (MismatchedInputException: "expected VALUE_STRING: need String... that
 * contains type id"), e essa exceção não tratada acabava virando 403 pro
 * cliente no endpoint público do cardápio. Reintroduzir cache aqui exige uma
 * configuração de serializer testada especificamente para esse formato (ex:
 * ObjectMapper dedicado sem tipagem polimórfica, já que o tipo de retorno é
 * sempre o mesmo e não precisa de "@class").
 */
@Service
@RequiredArgsConstructor
public class CardapioService {

    private final CategoriaService categoriaService;
    private final ProdutoService produtoService;

    public List<CardapioCategoriaResponse> getCardapio(Long restauranteId) {
        List<CategoriaResponse> categorias = categoriaService.listar(restauranteId);

        return categorias.stream()
            .filter(c -> c != null && c.isAtivo())
            .map(c -> {
                List<ProdutoResponse> produtos = produtoService
                    .listarPorCategoria(restauranteId, c.getId())
                    .stream()
                    .filter(p -> p != null && p.isAtivo())
                    .toList();
                return CardapioCategoriaResponse.builder()
                    .id(c.getId())
                    .nome(c.getNome())
                    .produtos(produtos)
                    .build();
            })
            .filter(c -> !c.getProdutos().isEmpty())
            .toList();
    }
}
