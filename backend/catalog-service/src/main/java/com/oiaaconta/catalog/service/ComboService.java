package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.ComboRequest;
import com.oiaaconta.catalog.dto.request.ItemQuantidadeRequest;
import com.oiaaconta.catalog.dto.response.ComboItemResponse;
import com.oiaaconta.catalog.dto.response.ComboResponse;
import com.oiaaconta.catalog.entity.Combo;
import com.oiaaconta.catalog.entity.ComboItem;
import com.oiaaconta.catalog.entity.Produto;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.ComboItemRepository;
import com.oiaaconta.catalog.repository.ComboRepository;
import com.oiaaconta.catalog.repository.ProdutoRepository;
import com.oiaaconta.catalog.util.ImagemValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComboService {

    private static final int IMAGEM_MAX_CHARS = 1_400_000;

    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;
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
        validarItens(restauranteId, request.getItens());
        Combo combo = comboRepository.save(Combo.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome()).descricao(request.getDescricao())
            .preco(request.getPreco())
            .imagemBase64(validarImagemOuLimpar(request.getImagemBase64(), null))
            .numeroCardapio(request.getNumeroCardapio())
            .ativo(true)
            .build());
        salvarItens(combo.getId(), request.getItens());
        return toResponse(combo, restauranteId);
    }

    @Transactional
    public ComboResponse atualizar(Long restauranteId, Long id, ComboRequest request) {
        Combo combo = buscarEntidade(restauranteId, id);
        validarItens(restauranteId, request.getItens());
        combo.setNome(request.getNome());
        combo.setDescricao(request.getDescricao());
        combo.setPreco(request.getPreco());
        combo.setImagemBase64(validarImagemOuLimpar(request.getImagemBase64(), combo.getImagemBase64()));
        combo.setNumeroCardapio(request.getNumeroCardapio());
        comboRepository.save(combo);
        comboItemRepository.deleteByComboId(id);
        // ComboItem usa IDENTITY, então o INSERT de salvarItens roda na hora
        // (não dá pra adiar até o commit) — sem o flush aqui, o DELETE acima
        // ainda não tinha sido de fato executado no banco, e reinserir um
        // item que já estava no combo batia na constraint única
        // (combo_id, produto_id) antes do delete ter efeito.
        comboItemRepository.flush();
        salvarItens(id, request.getItens());
        return toResponse(combo, restauranteId);
    }

    public ComboResponse alterarAtivo(Long restauranteId, Long id, boolean ativo) {
        Combo combo = buscarEntidade(restauranteId, id);
        combo.setAtivo(ativo);
        return toResponse(comboRepository.save(combo), restauranteId);
    }

    private void validarItens(Long restauranteId, List<ItemQuantidadeRequest> itens) {
        long distintos = itens.stream().map(ItemQuantidadeRequest::getProdutoId).distinct().count();
        if (distintos < 2) {
            throw new BusinessException("Um combo precisa de pelo menos 2 produtos distintos");
        }
        if (distintos != itens.size()) {
            throw new BusinessException("Não é possível repetir o mesmo produto em duas linhas do combo");
        }
        for (ItemQuantidadeRequest item : itens) {
            produtoRepository.findByIdAndRestauranteId(item.getProdutoId(), restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto #" + item.getProdutoId() + " não encontrado"));
        }
    }

    private void salvarItens(Long comboId, List<ItemQuantidadeRequest> itens) {
        for (ItemQuantidadeRequest item : itens) {
            comboItemRepository.save(ComboItem.builder()
                .comboId(comboId).produtoId(item.getProdutoId()).quantidade(item.getQuantidade())
                .build());
        }
    }

    Combo buscarEntidade(Long restauranteId, Long id) {
        return comboRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Combo não encontrado"));
    }

    private ComboResponse toResponse(Combo combo, Long restauranteId) {
        List<ComboItem> itens = comboItemRepository.findByComboId(combo.getId());
        Map<Long, Produto> produtos = new LinkedHashMap<>();
        for (ComboItem item : itens) {
            produtoRepository.findByIdAndRestauranteId(item.getProdutoId(), restauranteId)
                .ifPresent(p -> produtos.put(item.getProdutoId(), p));
        }
        List<ComboItemResponse> itensResponse = ratear(combo, itens, produtos);
        return ComboResponse.builder()
            .id(combo.getId()).restauranteId(combo.getRestauranteId())
            .nome(combo.getNome()).descricao(combo.getDescricao())
            .preco(combo.getPreco()).imagemBase64(combo.getImagemBase64())
            .numeroCardapio(combo.getNumeroCardapio())
            .ativo(combo.isAtivo()).itens(itensResponse)
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

    // Rateia o preço do combo entre os produtos que o compõem, proporcional ao
    // preço de tabela de cada um (resto de centavos vai pra primeira linha) —
    // usado pelo order-service pra expandir o combo em ItemPedido reais sem
    // duplicar essa conta.
    private List<ComboItemResponse> ratear(Combo combo, List<ComboItem> itens, Map<Long, Produto> produtos) {
        BigDecimal totalTabela = itens.stream()
            .map(i -> {
                Produto p = produtos.get(i.getProdutoId());
                BigDecimal preco = p != null ? p.getPreco() : BigDecimal.ZERO;
                return preco.multiply(BigDecimal.valueOf(i.getQuantidade()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ComboItemResponse> resultado = new java.util.ArrayList<>();
        BigDecimal somaAlocada = BigDecimal.ZERO;
        for (ComboItem item : itens) {
            Produto produto = produtos.get(item.getProdutoId());
            BigDecimal precoProduto = produto != null ? produto.getPreco() : BigDecimal.ZERO;
            BigDecimal valorTabelaLinha = precoProduto.multiply(BigDecimal.valueOf(item.getQuantidade()));

            BigDecimal alocado;
            if (totalTabela.compareTo(BigDecimal.ZERO) == 0) {
                // Fallback (não deveria ocorrer, produto.preco > 0 é obrigatório): divide igualmente.
                alocado = combo.getPreco().divide(BigDecimal.valueOf(itens.size()), 2, RoundingMode.HALF_UP);
            } else {
                alocado = combo.getPreco().multiply(valorTabelaLinha)
                    .divide(totalTabela, 2, RoundingMode.HALF_UP);
            }
            somaAlocada = somaAlocada.add(alocado);
            resultado.add(ComboItemResponse.builder()
                .produtoId(item.getProdutoId())
                .produtoNome(produto != null ? produto.getNome() : null)
                .quantidade(item.getQuantidade())
                .valorAlocado(alocado)
                .build());
        }

        BigDecimal diferenca = combo.getPreco().subtract(somaAlocada);
        if (diferenca.compareTo(BigDecimal.ZERO) != 0 && !resultado.isEmpty()) {
            ComboItemResponse primeiro = resultado.get(0);
            primeiro.setValorAlocado(primeiro.getValorAlocado().add(diferenca));
        }
        return resultado;
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
