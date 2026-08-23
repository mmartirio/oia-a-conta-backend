package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.response.CardapioCategoriaResponse;
import com.oiaaconta.catalog.dto.response.CardapioPublicoResponse;
import com.oiaaconta.catalog.dto.response.CategoriaResponse;
import com.oiaaconta.catalog.dto.response.ComboResponse;
import com.oiaaconta.catalog.dto.response.ProdutoResponse;
import com.oiaaconta.catalog.dto.response.PromocaoAplicavelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Monta o cardápio público (categorias ativas + produtos ativos de cada uma,
 * combos ativos e um selo de promoção geral).
 *
 * NÃO cacheado (removido de propósito): o valor retornado tem listas
 * aninhadas, e o GenericJackson2JsonRedisSerializer (tipagem polimórfica
 * automática do Jackson) se mostrou incompatível com esse formato em
 * produção — a escrita no cache funcionava, mas a leitura de volta falhava
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
    private final ComboService comboService;
    private final PromocaoService promocaoService;

    public CardapioPublicoResponse getCardapio(Long restauranteId) {
        List<CategoriaResponse> categorias = categoriaService.listar(restauranteId, false);

        List<CardapioCategoriaResponse> categoriasResp = categorias.stream()
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

        List<ComboResponse> combos = comboService.listar(restauranteId, true);

        // clienteId=null: só promoções com alvo TODOS entram aqui (a checagem
        // de GRUPO exige um cliente identificado, que o cardápio público não tem).
        List<PromocaoAplicavelResponse> promocoes = promocaoService.aplicaveis(restauranteId, null, null);
        PromocaoAplicavelResponse promocaoPublica = promocoes.isEmpty() ? null : promocoes.get(0);

        return CardapioPublicoResponse.builder()
            .categorias(categoriasResp)
            .combos(combos)
            .promocaoAtiva(promocaoPublica != null)
            .promocaoDescricao(promocaoPublica != null ? promocaoPublica.getNome() : null)
            .promocaoValorDesconto(promocaoPublica != null ? promocaoPublica.getValorDesconto() : null)
            .promocaoTipoDesconto(promocaoPublica != null ? promocaoPublica.getTipoDesconto().name() : null)
            .build();
    }
}
