package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.catalog.ComboGrupoDto;
import com.oiaaconta.order.dto.catalog.ComboGrupoProdutoDto;
import com.oiaaconta.order.dto.catalog.ComboResponseDto;
import com.oiaaconta.order.dto.request.EscolhaSaborRequest;
import com.oiaaconta.order.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Resolve os sabores de um combo (grupos com produtos elegíveis, ver
// ComboGrupoDto) na lista real de produtos a lançar no pedido — usado por
// EntregaService (WhatsApp/cardápio público) e PedidoService (comanda/PDV),
// que antes duplicavam essa mesma lógica de rateio de preço.
@Component
public class ComboEscolhaResolver {

    public record ItemResolvido(Long produtoId, String produtoNome, int quantidade, BigDecimal precoUnitario) {}

    public List<ItemResolvido> resolver(ComboResponseDto combo, List<EscolhaSaborRequest> escolhas, int comboQuantidade) {
        List<ComboGrupoDto> grupos = combo.getGrupos() != null ? combo.getGrupos() : List.of();

        Map<Long, ComboGrupoProdutoDto> produtosPorId = new LinkedHashMap<>();
        for (ComboGrupoDto g : grupos) {
            if (g.getProdutos() == null) continue;
            for (ComboGrupoProdutoDto p : g.getProdutos()) produtosPorId.putIfAbsent(p.getProdutoId(), p);
        }

        // (produtoId -> quantidade), agregada caso o mesmo sabor seja
        // escolhido em mais de um grupo ou repetido no mesmo grupo.
        Map<Long, Integer> quantidadePorProduto = new LinkedHashMap<>();
        boolean temEscolha = escolhas != null && !escolhas.isEmpty();
        if (temEscolha) {
            for (EscolhaSaborRequest e : escolhas) {
                if (!produtosPorId.containsKey(e.getProdutoId())) {
                    throw new BusinessException("Produto #" + e.getProdutoId() + " não é uma opção válida do combo '" + combo.getNome() + "'");
                }
                quantidadePorProduto.merge(e.getProdutoId(), e.getQuantidade(), Integer::sum);
            }
            int totalEscolhido = quantidadePorProduto.values().stream().mapToInt(Integer::intValue).sum();
            int totalEsperado = grupos.stream().mapToInt(ComboGrupoDto::getQuantidade).sum();
            if (totalEscolhido != totalEsperado) {
                throw new BusinessException("Escolha de sabores do combo '" + combo.getNome() + "' incompleta (esperado "
                    + totalEsperado + ", veio " + totalEscolhido + ")");
            }
        } else {
            // Sem escolha explícita (PDV/Garçom ainda não pedem sabor) — usa
            // o primeiro produto elegível de cada grupo como padrão.
            for (ComboGrupoDto g : grupos) {
                if (g.getProdutos() == null || g.getProdutos().isEmpty()) continue;
                quantidadePorProduto.merge(g.getProdutos().get(0).getProdutoId(), g.getQuantidade(), Integer::sum);
            }
        }

        // Rateia o preço fixo do combo entre os sabores escolhidos,
        // proporcional ao preço de tabela de cada um — a soma sempre bate
        // exatamente com combo.preco × comboQuantidade, então a escolha de
        // sabor nunca muda o valor total do combo.
        BigDecimal precoTotalCombo = combo.getPreco().multiply(BigDecimal.valueOf(comboQuantidade));
        BigDecimal totalTabela = quantidadePorProduto.entrySet().stream()
            .map(en -> produtosPorId.get(en.getKey()).getPreco().multiply(BigDecimal.valueOf(en.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> ids = new ArrayList<>(quantidadePorProduto.keySet());
        List<BigDecimal> alocados = new ArrayList<>();
        BigDecimal somaAlocada = BigDecimal.ZERO;
        for (Long produtoId : ids) {
            int qtd = quantidadePorProduto.get(produtoId);
            BigDecimal valorTabelaLinha = produtosPorId.get(produtoId).getPreco().multiply(BigDecimal.valueOf(qtd));
            BigDecimal alocado = totalTabela.compareTo(BigDecimal.ZERO) == 0
                ? precoTotalCombo.divide(BigDecimal.valueOf(ids.size()), 2, RoundingMode.HALF_UP)
                : precoTotalCombo.multiply(valorTabelaLinha).divide(totalTabela, 2, RoundingMode.HALF_UP);
            alocados.add(alocado);
            somaAlocada = somaAlocada.add(alocado);
        }
        BigDecimal diferenca = precoTotalCombo.subtract(somaAlocada);
        if (diferenca.compareTo(BigDecimal.ZERO) != 0 && !alocados.isEmpty()) {
            alocados.set(0, alocados.get(0).add(diferenca));
        }

        List<ItemResolvido> resultado = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Long produtoId = ids.get(i);
            int qtd = quantidadePorProduto.get(produtoId) * comboQuantidade;
            BigDecimal precoUnitario = alocados.get(i).divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP);
            resultado.add(new ItemResolvido(produtoId, produtosPorId.get(produtoId).getNome(), qtd, precoUnitario));
        }
        return resultado;
    }
}
