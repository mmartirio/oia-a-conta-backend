package com.oiaaconta.ifood.service;

import com.oiaaconta.ifood.client.CatalogClient;
import com.oiaaconta.ifood.client.IfoodCatalogClient;
import com.oiaaconta.ifood.dto.catalog.CardapioPublicoDto;
import com.oiaaconta.ifood.dto.catalog.CategoriaCardapioDto;
import com.oiaaconta.ifood.dto.catalog.ComboCardapioDto;
import com.oiaaconta.ifood.dto.catalog.ProdutoCardapioDto;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaResponse;
import com.oiaaconta.ifood.dto.ifood.IfoodItemRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodItemResponse;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    @Transactional
    public IfoodCatalogoSyncResponse sincronizar(Long restauranteId) {
        IfoodMerchant merchant = merchantAtivo(restauranteId);
        String token = "Bearer " + vinculoService.garantirTokenValido(merchant);
        String merchantId = merchant.getMerchantId();

        CardapioPublicoDto cardapio = catalogClient.buscarCardapio(restauranteId);

        Set<Long> produtosTocados = new HashSet<>();
        Set<Long> combosTocados = new HashSet<>();
        int categoriasSincronizadas = 0;
        int itensSincronizados = 0;

        List<CategoriaCardapioDto> categorias = cardapio.getCategorias() != null ? cardapio.getCategorias() : List.of();
        for (CategoriaCardapioDto categoria : categorias) {
            String ifoodCategoryId = sincronizarCategoria(restauranteId, token, merchantId, categoria.getId(), categoria.getNome());
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
            String categoriaCombosId = sincronizarCategoria(restauranteId, token, merchantId, CATEGORIA_COMBOS_ID, "Combos");
            categoriasSincronizadas++;
            for (ComboCardapioDto combo : combos) {
                sincronizarItem(restauranteId, token, merchantId, TIPO_COMBO, combo.getId(),
                    combo.getNome(), combo.getDescricao(), combo.getPreco(), categoriaCombosId);
                combosTocados.add(combo.getId());
                itensSincronizados++;
            }
        }

        int pausados = pausarNaoTocados(restauranteId, token, merchantId, TIPO_PRODUTO, produtosTocados)
            + pausarNaoTocados(restauranteId, token, merchantId, TIPO_COMBO, combosTocados);

        merchant.setCatalogoSincronizadoEm(LocalDateTime.now());
        merchantRepository.save(merchant);

        return IfoodCatalogoSyncResponse.builder()
            .categoriasSincronizadas(categoriasSincronizadas)
            .itensSincronizados(itensSincronizados)
            .itensPausados(pausados)
            .sincronizadoEm(merchant.getCatalogoSincronizadoEm())
            .build();
    }

    private String sincronizarCategoria(Long restauranteId, String token, String merchantId, Long categoriaId, String nome) {
        Optional<IfoodMapeamento> existente = mapeamentoRepository
            .findByRestauranteIdAndTipoAndLocalId(restauranteId, TIPO_CATEGORIA, categoriaId);
        IfoodCategoriaRequest body = new IfoodCategoriaRequest(nome, "categoria-" + categoriaId);

        if (existente.isPresent()) {
            ifoodCatalogClient.atualizarCategoria(token, merchantId, existente.get().getIfoodId(), body);
            existente.get().setAtualizadoEm(LocalDateTime.now());
            mapeamentoRepository.save(existente.get());
            return existente.get().getIfoodId();
        }

        IfoodCategoriaResponse resp = ifoodCatalogClient.criarCategoria(token, merchantId, body);
        mapeamentoRepository.save(IfoodMapeamento.builder()
            .restauranteId(restauranteId).tipo(TIPO_CATEGORIA).localId(categoriaId)
            .ifoodId(resp.getId()).atualizadoEm(LocalDateTime.now()).build());
        return resp.getId();
    }

    private void sincronizarItem(Long restauranteId, String token, String merchantId, String tipo, Long localId,
                                  String nome, String descricao, BigDecimal preco, String ifoodCategoryId) {
        Optional<IfoodMapeamento> existente = mapeamentoRepository
            .findByRestauranteIdAndTipoAndLocalId(restauranteId, tipo, localId);
        IfoodItemRequest body = new IfoodItemRequest(
            (TIPO_PRODUTO.equals(tipo) ? "produto-" : "combo-") + localId,
            ifoodCategoryId, nome, descricao, new IfoodItemRequest.Preco(preco), "AVAILABLE");

        if (existente.isPresent()) {
            ifoodCatalogClient.atualizarItem(token, merchantId, existente.get().getIfoodId(), body);
            existente.get().setAtualizadoEm(LocalDateTime.now());
            mapeamentoRepository.save(existente.get());
            return;
        }

        IfoodItemResponse resp = ifoodCatalogClient.criarItem(token, merchantId, body);
        mapeamentoRepository.save(IfoodMapeamento.builder()
            .restauranteId(restauranteId).tipo(tipo).localId(localId)
            .ifoodId(resp.getId()).atualizadoEm(LocalDateTime.now()).build());
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
                ifoodCatalogClient.atualizarStatusItem(token, merchantId, m.getIfoodId(), Map.of("status", "UNAVAILABLE"));
            } catch (Exception e) {
                log.warn("Falha ao pausar item iFood {} (restaurante {}): {}", m.getIfoodId(), restauranteId, e.getMessage());
            }
        }
        return existentes.size();
    }

    private IfoodMerchant merchantAtivo(Long restauranteId) {
        return merchantRepository.findByRestauranteId(restauranteId)
            .filter(IfoodMerchant::isAtivo)
            .orElseThrow(() -> new BusinessException("Restaurante não está vinculado ao iFood"));
    }
}
