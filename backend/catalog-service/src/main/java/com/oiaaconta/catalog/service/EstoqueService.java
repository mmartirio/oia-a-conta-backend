package com.oiaaconta.catalog.service;

import com.oiaaconta.catalog.dto.request.EstoqueBaixaRequest;
import com.oiaaconta.catalog.dto.request.EstoqueConfigRequest;
import com.oiaaconta.catalog.dto.request.ItemQuantidadeRequest;
import com.oiaaconta.catalog.dto.request.MovimentoEstoqueRequest;
import com.oiaaconta.catalog.dto.response.EstoqueResponse;
import com.oiaaconta.catalog.dto.response.MovimentacaoEstoqueResponse;
import com.oiaaconta.catalog.entity.Estoque;
import com.oiaaconta.catalog.entity.MovimentacaoEstoque;
import com.oiaaconta.catalog.entity.Produto;
import com.oiaaconta.catalog.enums.TipoMovimentacaoEstoque;
import com.oiaaconta.catalog.exception.BusinessException;
import com.oiaaconta.catalog.exception.ResourceNotFoundException;
import com.oiaaconta.catalog.repository.EstoqueRepository;
import com.oiaaconta.catalog.repository.MovimentacaoEstoqueRepository;
import com.oiaaconta.catalog.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;

    public List<EstoqueResponse> listar(Long restauranteId) {
        List<Produto> produtos = produtoRepository.findByRestauranteIdOrderByNomeAsc(restauranteId);
        Map<Long, Estoque> porProduto = new HashMap<>();
        estoqueRepository.findByRestauranteId(restauranteId).forEach(e -> porProduto.put(e.getProdutoId(), e));
        return produtos.stream().map(p -> toResponse(p, porProduto.get(p.getId()))).toList();
    }

    public List<EstoqueResponse> alertas(Long restauranteId) {
        return listar(restauranteId).stream()
            .filter(EstoqueResponse::isAbaixoDoMinimo)
            .toList();
    }

    public EstoqueResponse buscarPorProduto(Long restauranteId, Long produtoId) {
        Produto produto = produtoRepository.findByIdAndRestauranteId(produtoId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        return toResponse(produto, estoqueRepository.findByProdutoIdAndRestauranteId(produtoId, restauranteId).orElse(null));
    }

    public EstoqueResponse configurar(Long restauranteId, Long produtoId, EstoqueConfigRequest request) {
        produtoRepository.findByIdAndRestauranteId(produtoId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        Estoque estoque = buscarOuCriar(restauranteId, produtoId);
        estoque.setQuantidadeMinima(request.getQuantidadeMinima());
        estoque.setControlado(request.getControlado());
        estoqueRepository.save(estoque);
        return buscarPorProduto(restauranteId, produtoId);
    }

    // Movimentação manual (ENTRADA/SAIDA/AJUSTE) feita pelo admin na tela de estoque.
    @Transactional
    public EstoqueResponse movimentar(Long restauranteId, Long produtoId, MovimentoEstoqueRequest request,
                                       Long usuarioId, String usuarioNome) {
        produtoRepository.findByIdAndRestauranteId(produtoId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        Estoque estoque = buscarOuCriar(restauranteId, produtoId);

        int quantidadeAnterior = estoque.getQuantidade();
        int delta;
        switch (request.getTipo()) {
            case ENTRADA -> delta = request.getQuantidade();
            case SAIDA -> delta = -request.getQuantidade();
            case AJUSTE -> delta = request.getQuantidade() - quantidadeAnterior; // AJUSTE fixa o saldo absoluto informado
            default -> throw new BusinessException("Tipo de movimentação manual inválido: " + request.getTipo());
        }
        int novaQuantidade = quantidadeAnterior + delta;
        if (novaQuantidade < 0) {
            throw new BusinessException("Saldo insuficiente: estoque atual é " + quantidadeAnterior);
        }
        // Entrada com custo (compra de insumo): atualiza o custo médio
        // ponderado do saldo — (saldo × custo médio + qtd × custo) / novo saldo.
        java.math.BigDecimal custoUnitario = null;
        java.math.BigDecimal valorTotal = null;
        if (request.getTipo() == TipoMovimentacaoEstoque.ENTRADA && request.getCustoUnitario() != null) {
            custoUnitario = request.getCustoUnitario();
            valorTotal = custoUnitario.multiply(java.math.BigDecimal.valueOf(delta)).setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal custoAnterior = estoque.getCustoMedio() != null ? estoque.getCustoMedio() : custoUnitario;
            java.math.BigDecimal base = custoAnterior.multiply(java.math.BigDecimal.valueOf(Math.max(quantidadeAnterior, 0)));
            estoque.setCustoMedio(novaQuantidade > 0
                ? base.add(valorTotal).divide(java.math.BigDecimal.valueOf(novaQuantidade), 4, java.math.RoundingMode.HALF_UP)
                : custoUnitario);
        }
        estoque.setQuantidade(novaQuantidade);
        estoqueRepository.save(estoque);

        movimentacaoRepository.save(MovimentacaoEstoque.builder()
            .restauranteId(restauranteId).produtoId(produtoId)
            .tipo(request.getTipo()).quantidade(delta).quantidadeResultante(novaQuantidade)
            .motivo(request.getMotivo())
            .custoUnitario(custoUnitario).valorTotal(valorTotal)
            .criadoPorId(usuarioId).criadoPorNome(usuarioNome)
            .build());

        return buscarPorProduto(restauranteId, produtoId);
    }

    public Page<MovimentacaoEstoqueResponse> listarMovimentacoes(Long restauranteId, Long produtoId, Pageable pageable) {
        return movimentacaoRepository.findByProdutoIdAndRestauranteIdOrderByCriadoEmDesc(produtoId, restauranteId, pageable)
            .map(this::toResponse);
    }

    // Verifica e baixa estoque em lote, atômico (tudo ou nada) — usado pelo
    // order-service no momento da venda. Itens de produto não "controlado"
    // (sem estoque ativado) são ignorados, sempre permitidos.
    @Transactional
    public void verificarEBaixar(Long restauranteId, EstoqueBaixaRequest request) {
        for (ItemQuantidadeRequest item : request.getItens()) {
            Estoque estoque = estoqueRepository.findByProdutoIdAndRestauranteId(item.getProdutoId(), restauranteId).orElse(null);
            if (estoque == null || !estoque.isControlado()) continue;

            int linhasAfetadas = estoqueRepository.baixarSeHouverSaldo(item.getProdutoId(), restauranteId, item.getQuantidade());
            if (linhasAfetadas == 0) {
                Produto produto = produtoRepository.findByIdAndRestauranteId(item.getProdutoId(), restauranteId).orElse(null);
                String nome = produto != null ? produto.getNome() : ("produto #" + item.getProdutoId());
                throw new BusinessException("Estoque insuficiente para: " + nome + " (disponível: " + estoque.getQuantidade()
                    + ", solicitado: " + item.getQuantidade() + ")");
            }

            Estoque atualizado = estoqueRepository.findByProdutoIdAndRestauranteId(item.getProdutoId(), restauranteId).orElseThrow();
            movimentacaoRepository.save(MovimentacaoEstoque.builder()
                .restauranteId(restauranteId).produtoId(item.getProdutoId())
                .tipo(TipoMovimentacaoEstoque.VENDA).quantidade(-item.getQuantidade())
                .quantidadeResultante(atualizado.getQuantidade())
                .referenciaExterna(request.getReferenciaExterna())
                .build());
        }
    }

    // Compensação (best-effort, chamada pelo order-service ao cancelar/estornar
    // uma venda que já tinha baixado estoque). Também ignora produto não controlado.
    @Transactional
    public void liberar(Long restauranteId, EstoqueBaixaRequest request) {
        for (ItemQuantidadeRequest item : request.getItens()) {
            Estoque estoque = estoqueRepository.findByProdutoIdAndRestauranteId(item.getProdutoId(), restauranteId).orElse(null);
            if (estoque == null || !estoque.isControlado()) continue;

            estoqueRepository.devolver(item.getProdutoId(), restauranteId, item.getQuantidade());
            Estoque atualizado = estoqueRepository.findByProdutoIdAndRestauranteId(item.getProdutoId(), restauranteId).orElseThrow();
            movimentacaoRepository.save(MovimentacaoEstoque.builder()
                .restauranteId(restauranteId).produtoId(item.getProdutoId())
                .tipo(TipoMovimentacaoEstoque.ESTORNO).quantidade(item.getQuantidade())
                .quantidadeResultante(atualizado.getQuantidade())
                .referenciaExterna(request.getReferenciaExterna())
                .build());
        }
    }

    private Estoque buscarOuCriar(Long restauranteId, Long produtoId) {
        return estoqueRepository.findByProdutoIdAndRestauranteId(produtoId, restauranteId)
            .orElseGet(() -> estoqueRepository.save(Estoque.builder()
                .restauranteId(restauranteId).produtoId(produtoId)
                .quantidade(0).quantidadeMinima(0).controlado(false)
                .build()));
    }

    // Aba Insumos do Financeiro: entradas (compras) do período com custo e o
    // valor atual do estoque (saldo × custo médio de cada produto).
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> resumoFinanceiro(Long restauranteId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        java.util.Map<Long, String> nomes = new java.util.HashMap<>();
        produtoRepository.findByRestauranteIdOrderByNomeAsc(restauranteId).forEach(p -> nomes.put(p.getId(), p.getNome()));

        java.util.List<java.util.Map<String, Object>> entradas = new java.util.ArrayList<>();
        java.math.BigDecimal totalEntradas = java.math.BigDecimal.ZERO;
        int semCusto = 0;
        for (MovimentacaoEstoque m : movimentacaoRepository
                .findByRestauranteIdAndTipoAndCriadoEmBetweenOrderByCriadoEmDesc(restauranteId, TipoMovimentacaoEstoque.ENTRADA, inicio, fim)) {
            java.util.Map<String, Object> e = new java.util.LinkedHashMap<>();
            e.put("id", m.getId());
            e.put("produtoId", m.getProdutoId());
            e.put("produtoNome", nomes.getOrDefault(m.getProdutoId(), "Produto #" + m.getProdutoId()));
            e.put("quantidade", m.getQuantidade());
            e.put("custoUnitario", m.getCustoUnitario());
            e.put("valorTotal", m.getValorTotal());
            e.put("motivo", m.getMotivo());
            e.put("criadoPorNome", m.getCriadoPorNome());
            e.put("criadoEm", m.getCriadoEm());
            entradas.add(e);
            if (m.getValorTotal() != null) totalEntradas = totalEntradas.add(m.getValorTotal());
            else semCusto++;
        }

        java.math.BigDecimal valorEmEstoque = java.math.BigDecimal.ZERO;
        java.util.List<java.util.Map<String, Object>> itensEstoque = new java.util.ArrayList<>();
        for (Estoque est : estoqueRepository.findByRestauranteIdAndControladoTrue(restauranteId)) {
            if (est.getCustoMedio() == null || est.getQuantidade() <= 0) continue;
            java.math.BigDecimal valor = est.getCustoMedio().multiply(java.math.BigDecimal.valueOf(est.getQuantidade()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
            valorEmEstoque = valorEmEstoque.add(valor);
            java.util.Map<String, Object> i2 = new java.util.LinkedHashMap<>();
            i2.put("produtoId", est.getProdutoId());
            i2.put("produtoNome", nomes.getOrDefault(est.getProdutoId(), "Produto #" + est.getProdutoId()));
            i2.put("quantidade", est.getQuantidade());
            i2.put("custoMedio", est.getCustoMedio());
            i2.put("valor", valor);
            itensEstoque.add(i2);
        }
        itensEstoque.sort((a, b) -> ((java.math.BigDecimal) b.get("valor")).compareTo((java.math.BigDecimal) a.get("valor")));

        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("totalEntradas", totalEntradas);
        r.put("qtdEntradas", entradas.size());
        r.put("entradasSemCusto", semCusto);
        r.put("valorEmEstoque", valorEmEstoque);
        r.put("entradas", entradas);
        r.put("itensEstoque", itensEstoque);
        return r;
    }

    private EstoqueResponse toResponse(Produto produto, Estoque estoque) {
        int quantidade = estoque != null ? estoque.getQuantidade() : 0;
        int quantidadeMinima = estoque != null ? estoque.getQuantidadeMinima() : 0;
        boolean controlado = estoque != null && estoque.isControlado();
        return EstoqueResponse.builder()
            .produtoId(produto.getId()).produtoNome(produto.getNome())
            .quantidade(quantidade).quantidadeMinima(quantidadeMinima)
            .controlado(controlado)
            .abaixoDoMinimo(controlado && quantidade <= quantidadeMinima)
            .custoMedio(estoque != null ? estoque.getCustoMedio() : null)
            .valorEmEstoque(estoque != null && estoque.getCustoMedio() != null && quantidade > 0
                ? estoque.getCustoMedio().multiply(java.math.BigDecimal.valueOf(quantidade)).setScale(2, java.math.RoundingMode.HALF_UP)
                : null)
            .build();
    }

    private MovimentacaoEstoqueResponse toResponse(MovimentacaoEstoque m) {
        return MovimentacaoEstoqueResponse.builder()
            .id(m.getId()).produtoId(m.getProdutoId()).tipo(m.getTipo())
            .quantidade(m.getQuantidade()).quantidadeResultante(m.getQuantidadeResultante())
            .motivo(m.getMotivo()).referenciaExterna(m.getReferenciaExterna())
            .custoUnitario(m.getCustoUnitario()).valorTotal(m.getValorTotal())
            .criadoPorNome(m.getCriadoPorNome()).criadoEm(m.getCriadoEm())
            .build();
    }
}
