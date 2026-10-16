package com.oiaaconta.ifood.service;

import com.oiaaconta.ifood.client.CatalogClient;
import com.oiaaconta.ifood.client.IfoodCatalogClient;
import com.oiaaconta.ifood.dto.catalog.CardapioPublicoDto;
import com.oiaaconta.ifood.dto.catalog.CategoriaCardapioDto;
import com.oiaaconta.ifood.dto.catalog.ComboCardapioDto;
import com.oiaaconta.ifood.dto.catalog.ProdutoCardapioDto;
import com.oiaaconta.ifood.dto.ifood.IfoodCatalogDto;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaResponse;
import com.oiaaconta.ifood.dto.ifood.IfoodItemRequest;
import com.oiaaconta.ifood.dto.response.IfoodCatalogoSyncResponse;
import com.oiaaconta.ifood.entity.IfoodMapeamento;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.exception.BusinessException;
import com.oiaaconta.ifood.repository.IfoodMapeamentoRepository;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Sincroniza categorias/produtos/combos do cardápio (já expostos
// publicamente pelo catalog-service) pro catálogo do iFood. Sem suporte a
// grupos de opção/complementos — fora de escopo, nosso catálogo não modela
// isso. Combos entram como item de preço fixo numa categoria sintética
// "Combos", já que na nossa modelagem eles não pertencem a nenhuma
// Categoria.
@Service
@RequiredArgsConstructor
@Slf4j
public class IfoodCatalogSyncService {

    private static final String TIPO_CATEGORIA = "CATEGORIA";
    private static final String TIPO_PRODUTO = "PRODUTO";
    private static final String TIPO_COMBO = "COMBO";
    private static final long CATEGORIA_COMBOS_ID = -1L;

    private final CatalogClient catalogClient;
    private final IfoodCatalogClient ifoodCatalogClient;
    private final IfoodMerchantRepository merchantRepository;
    private final IfoodMapeamentoRepository mapeamentoRepository;
    private final IfoodVinculoService vinculoService;

    // Chamado pelo catalog-service (best-effort, engolido em try/catch pelo
    // call-site) toda vez que um produto/combo é criado, editado ou
    // desativado — mantém o catálogo do iFood em dia sem esperar o
    // scheduler periódico ou o botão "Sincronizar agora". Não-op silencioso
    // pra restaurante sem vínculo ativo com o iFood (a grande maioria).
    @Transactional
    public void sincronizarSeVinculado(Long restauranteId) {
        boolean vinculado = merchantRepository.findByRestauranteId(restauranteId)
            .map(IfoodMerchant::isAtivo).orElse(false);
        if (!vinculado) return;
        sincronizar(restauranteId);
    }

    @Transactional
    public IfoodCatalogoSyncResponse sincronizar(Long restauranteId) {
        IfoodMerchant merchant = merchantAtivo(restauranteId);
        String token = "Bearer " + vinculoService.garantirTokenValido(merchant);
        String merchantId = merchant.getMerchantId();
        String catalogId = catalogIdDaLoja(token, merchantId);

        CardapioPublicoDto cardapio = catalogClient.buscarCardapio(restauranteId);

        Set<Long> categoriasTocadas = new HashSet<>();
        Set<Long> produtosTocados = new HashSet<>();
        Set<Long> combosTocados = new HashSet<>();
        int categoriasSincronizadas = 0;
        int itensSincronizados = 0;

        List<CategoriaCardapioDto> categorias = cardapio.getCategorias() != null ? cardapio.getCategorias() : List.of();
        for (CategoriaCardapioDto categoria : categorias) {
            String ifoodCategoryId = sincronizarCategoria(restauranteId, token, merchantId, catalogId, categoria.getId(), categoria.getNome());
            categoriasTocadas.add(categoria.getId());
            categoriasSincronizadas++;
            List<ProdutoCardapioDto> produtos = categoria.getProdutos() != null ? categoria.getProdutos() : List.of();
            for (ProdutoCardapioDto produto : produtos) {
                sincronizarItem(restauranteId, token, merchantId, TIPO_PRODUTO, produto.getId(),
                    produto.getNome(), produto.getDescricao(), produto.getPreco(), ifoodCategoryId);
                produtosTocados.add(produto.getId());
                itensSincronizados++;
            }
        }

        List<ComboCardapioDto> combos = cardapio.getCombos() != null ? cardapio.getCombos() : List.of();
        if (!combos.isEmpty()) {
            String categoriaCombosId = sincronizarCategoria(restauranteId, token, merchantId, catalogId, CATEGORIA_COMBOS_ID, "Combos");
            categoriasTocadas.add(CATEGORIA_COMBOS_ID);
            categoriasSincronizadas++;
            for (ComboCardapioDto combo : combos) {
                sincronizarItem(restauranteId, token, merchantId, TIPO_COMBO, combo.getId(),
                    combo.getNome(), combo.getDescricao(), combo.getPreco(), categoriaCombosId);
                combosTocados.add(combo.getId());
                itensSincronizados++;
            }
        }

        int pausados = pausarNaoTocados(restauranteId, token, merchantId, TIPO_PRODUTO, produtosTocados)
            + pausarNaoTocados(restauranteId, token, merchantId, TIPO_COMBO, combosTocados)
            + pausarCategoriasNaoTocadas(restauranteId, token, merchantId, catalogId, categoriasTocadas);

        merchant.setCatalogoSincronizadoEm(LocalDateTime.now());
        merchantRepository.save(merchant);

        return IfoodCatalogoSyncResponse.builder()
            .categoriasSincronizadas(categoriasSincronizadas)
            .itensSincronizados(itensSincronizados)
            .itensPausados(pausados)
            .sincronizadoEm(merchant.getCatalogoSincronizadoEm())
            .build();
    }

    private String sincronizarCategoria(Long restauranteId, String token, String merchantId, String catalogId, Long categoriaId, String nome) {
        Optional<IfoodMapeamento> existente = mapeamentoRepository
            .findByRestauranteIdAndTipoAndLocalId(restauranteId, TIPO_CATEGORIA, categoriaId);
        IfoodCategoriaRequest body = new IfoodCategoriaRequest(nome, "AVAILABLE", "DEFAULT", 0);

        if (existente.isPresent()) {
            ifoodCatalogClient.atualizarCategoria(token, merchantId, catalogId, existente.get().getIfoodId(), body);
            existente.get().setAtualizadoEm(LocalDateTime.now());
            mapeamentoRepository.save(existente.get());
            return existente.get().getIfoodId();
        }

        IfoodCategoriaResponse resp = ifoodCatalogClient.criarCategoria(token, merchantId, catalogId, body);
        mapeamentoRepository.save(IfoodMapeamento.builder()
            .restauranteId(restauranteId).tipo(TIPO_CATEGORIA).localId(categoriaId)
            .ifoodId(resp.getId()).atualizadoEm(LocalDateTime.now()).build());
        return resp.getId();
    }

    // Cria OU atualiza — PUT /items é idempotente por item.id, então usamos
    // sempre a mesma chamada; o id do item (guardado em IfoodMapeamento) é
    // gerado por nós na primeira vez e reaproveitado depois, e o id do
    // "product" embutido é derivado dele de forma determinística (não temos
    // coluna própria pra guardar um segundo id, e não precisamos: o product
    // é sub-recurso do item, criado/atualizado junto a cada PUT).
    private void sincronizarItem(Long restauranteId, String token, String merchantId, String tipo, Long localId,
                                  String nome, String descricao, BigDecimal preco, String ifoodCategoryId) {
        Optional<IfoodMapeamento> existente = mapeamentoRepository
            .findByRestauranteIdAndTipoAndLocalId(restauranteId, tipo, localId);
        String externalCode = (TIPO_PRODUTO.equals(tipo) ? "produto-" : "combo-") + localId;
        String itemId = existente.map(IfoodMapeamento::getIfoodId).orElseGet(() -> UUID.randomUUID().toString());
        String productId = UUID.nameUUIDFromBytes(("product-" + itemId).getBytes(StandardCharsets.UTF_8)).toString();

        IfoodItemRequest body = new IfoodItemRequest(
            new IfoodItemRequest.Item(itemId, "DEFAULT", ifoodCategoryId, "AVAILABLE",
                new IfoodItemRequest.Preco(preco), externalCode, productId),
            List.of(new IfoodItemRequest.Produto(productId, nome, descricao, externalCode)));

        ifoodCatalogClient.salvarItem(token, merchantId, body);

        if (existente.isPresent()) {
            existente.get().setAtualizadoEm(LocalDateTime.now());
            mapeamentoRepository.save(existente.get());
        } else {
            mapeamentoRepository.save(IfoodMapeamento.builder()
                .restauranteId(restauranteId).tipo(tipo).localId(localId)
                .ifoodId(itemId).atualizadoEm(LocalDateTime.now()).build());
        }
    }

    // Itens já sincronizados antes mas que não vieram nesta rodada do
    // cardápio (produto/combo desativado por aqui) ficam UNAVAILABLE no
    // iFood — o mapeamento não é apagado, só pausa, pra reativar sem perder
    // o vínculo se o item voltar a ficar ativo.
    private int pausarNaoTocados(Long restauranteId, String token, String merchantId, String tipo, Set<Long> tocados) {
        List<IfoodMapeamento> existentes = mapeamentoRepository.findByRestauranteIdAndTipo(restauranteId, tipo).stream()
            .filter(m -> !tocados.contains(m.getLocalId()))
            .toList();
        for (IfoodMapeamento m : existentes) {
            try {
                ifoodCatalogClient.atualizarStatusItem(token, merchantId,
                    Map.of("itemId", m.getIfoodId(), "status", "UNAVAILABLE"));
            } catch (Exception e) {
                log.warn("Falha ao pausar item iFood {} (restaurante {}): {}", m.getIfoodId(), restauranteId, e.getMessage());
            }
        }
        return existentes.size();
    }

    // Categorias já sincronizadas antes mas que não vieram nesta rodada
    // (categoria excluída aqui, ou "Combos" sem nenhum combo ativo no
    // momento) ficam UNAVAILABLE no iFood — mesmo tratamento dado a
    // produtos/combos em pausarNaoTocados, pro cardápio de lá refletir
    // sempre o cardápio local, sem deixar categoria órfã visível pro
    // cliente.
    private int pausarCategoriasNaoTocadas(Long restauranteId, String token, String merchantId, String catalogId, Set<Long> tocadas) {
        List<IfoodMapeamento> existentes = mapeamentoRepository.findByRestauranteIdAndTipo(restauranteId, TIPO_CATEGORIA).stream()
            .filter(m -> !tocadas.contains(m.getLocalId()))
            .toList();
        for (IfoodMapeamento m : existentes) {
            try {
                ifoodCatalogClient.atualizarStatusCategoria(token, merchantId, catalogId, m.getIfoodId(), Map.of("status", "UNAVAILABLE"));
            } catch (Exception e) {
                log.warn("Falha ao pausar categoria iFood {} (restaurante {}): {}", m.getIfoodId(), restauranteId, e.getMessage());
            }
        }
        return existentes.size();
    }

    // Cada merchant pode ter mais de um catálogo (ex: contexto DEFAULT pra
    // delivery, INDOOR pra consumo local) — usamos sempre o primeiro
    // retornado (contexto DEFAULT na prática, pro tipo de loja de teste),
    // já que hoje o catálogo daqui não distingue canal.
    private String catalogIdDaLoja(String token, String merchantId) {
        List<IfoodCatalogDto> catalogos = ifoodCatalogClient.listarCatalogos(token, merchantId);
        if (catalogos == null || catalogos.isEmpty()) {
            throw new BusinessException("Nenhum catálogo encontrado pra essa loja no iFood");
        }
        return catalogos.get(0).getCatalogId();
    }

    private IfoodMerchant merchantAtivo(Long restauranteId) {
        return merchantRepository.findByRestauranteId(restauranteId)
            .filter(IfoodMerchant::isAtivo)
            .orElseThrow(() -> new BusinessException("Restaurante não está vinculado ao iFood"));
    }
}
