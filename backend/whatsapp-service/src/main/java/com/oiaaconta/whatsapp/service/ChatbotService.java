package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.client.CatalogClient;
import com.oiaaconta.whatsapp.client.EvolutionApiClient;
import com.oiaaconta.whatsapp.client.OrderClient;
import com.oiaaconta.whatsapp.entity.ItemCarrinho;
import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import com.oiaaconta.whatsapp.enums.EstadoSessao;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final SessaoWhatsappRepository sessaoRepo;
    private final EvolutionApiClient evolutionClient;
    private final CatalogClient catalogClient;
    private final OrderClient orderClient;
    private final MensagemTemplateService mensagemService;

    private static final int MAX_ERROS = 3;

    @Async
    @Transactional
    @SuppressWarnings("null")
    public void processarMensagem(String telefone, String texto, Long restauranteId, String pushName) {
        if (texto == null || texto.isBlank()) return;
        String textoLimpo = texto.trim().toLowerCase();

        SessaoWhatsapp sessao = sessaoRepo.findByTelefoneAndRestauranteId(telefone, restauranteId)
            .orElseGet(() -> SessaoWhatsapp.builder()
                .telefone(telefone)
                .restauranteId(restauranteId)
                .estado(EstadoSessao.INICIO)
                .build());

        if (textoLimpo.equals("cancelar") || textoLimpo.equals("sair")) {
            resetarSessao(sessao);
            sessaoRepo.save(sessao);
            enviar(telefone, mensagemService.resolverTexto(restauranteId, "CHATBOT_CANCELADO", null));
            return;
        }

        try {
            switch (sessao.getEstado()) {
                case INICIO -> processarInicio(sessao, pushName);
                case COLETANDO_NOME -> processarNome(sessao, texto);
                case COLETANDO_ENDERECO -> processarEndereco(sessao, texto);
                case NAVEGANDO_CATEGORIAS -> processarCategoria(sessao, textoLimpo);
                case NAVEGANDO_PRODUTOS -> processarProduto(sessao, textoLimpo);
                case AGUARDANDO_QUANTIDADE -> processarQuantidade(sessao, textoLimpo);
                case REVISANDO_CARRINHO -> processarRevisao(sessao, textoLimpo);
                case COLETANDO_PAGAMENTO -> processarPagamento(sessao, textoLimpo);
                case COLETANDO_OBSERVACAO -> processarObservacao(sessao, textoLimpo);
                case CONFIRMANDO_PEDIDO -> processarConfirmacao(sessao, textoLimpo);
                case PEDIDO_ENVIADO, AGUARDANDO_PIX -> enviar(telefone,
                    mensagemService.resolverTexto(sessao.getRestauranteId(), "CHATBOT_JA_ENVIADO", null));
                // estados legados de endereço fracionado — reinicia o fluxo
                default -> processarInicio(sessao, pushName);
            }
            sessao.setErrosConsecutivos(0);
        } catch (EntradaInvalidaException e) {
            int erros = sessao.getErrosConsecutivos() + 1;
            sessao.setErrosConsecutivos(erros);
            if (erros >= MAX_ERROS) {
                sessao.setErrosConsecutivos(0);
                enviar(telefone, "Não entendi sua resposta. " + e.getMessage() + "\n\nDigite *cancelar* a qualquer momento para recomeçar.");
            } else {
                enviar(telefone, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Erro no chatbot para {}: {}", telefone, e.getMessage(), e);
            enviar(telefone, "Ocorreu um erro. Tente novamente ou digite *cancelar* para recomeçar.");
        }

        sessaoRepo.save(sessao);
    }

    private void processarInicio(SessaoWhatsapp s, String pushName) {
        resetarSessao(s);
        if (pushName != null && !pushName.isBlank()) {
            s.setClienteNome(pushName.trim());
            s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
            String msg = mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_BOAS_VINDAS",
                Map.of("NOME", pushName.trim()));
            enviar(s.getTelefone(), msg);
            mostrarCategorias(s);
        } else {
            s.setEstado(EstadoSessao.COLETANDO_NOME);
            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_NOME", null));
        }
    }

    private void processarNome(SessaoWhatsapp s, String texto) {
        if (texto.trim().length() < 2) throw new EntradaInvalidaException("Nome muito curto. Informe seu nome:");
        s.setClienteNome(texto.trim());
        s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
        String msg = mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_SAUDACAO_APOS_NOME",
            Map.of("NOME", s.getClienteNome()));
        enviar(s.getTelefone(), msg);
        mostrarCategorias(s);
    }

    private void processarEndereco(SessaoWhatsapp s, String texto) {
        if (texto.trim().length() < 5) {
            throw new EntradaInvalidaException(
                "Por favor, informe um endereço válido.\n_Ex: Rua das Flores, 123, Centro, Aracaju_");
        }
        s.setEnderecoRua(texto.trim());
        s.setEnderecoNumero(null);
        s.setEnderecoBairro(null);
        s.setEnderecoCidade(null);
        s.setEnderecoComplemento(null);
        s.setEstado(EstadoSessao.COLETANDO_PAGAMENTO);
        enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_PAGAMENTO", null));
    }

    private void mostrarCategorias(SessaoWhatsapp s) {
        try {
            List<CatalogClient.CategoriaDto> categorias = catalogClient.listarCategorias(s.getRestauranteId())
                .stream().filter(CatalogClient.CategoriaDto::isAtivo).toList();

            if (categorias.isEmpty()) {
                enviar(s.getTelefone(), "Desculpe, o cardápio está vazio no momento.");
                return;
            }

            StringBuilder sb = new StringBuilder("*Cardápio* 🍽️\n\nEscolha uma categoria:\n\n");
            for (int i = 0; i < categorias.size(); i++) {
                sb.append(i + 1).append(". ").append(categorias.get(i).getNome()).append("\n");
            }
            if (!s.getItens().isEmpty()) {
                sb.append("\nOu digite *ver carrinho* para revisar seu pedido.");
            }
            enviar(s.getTelefone(), sb.toString());
        } catch (Exception e) {
            log.error("Erro ao buscar categorias para restaurante {}: {}", s.getRestauranteId(), e.getMessage(), e);
            enviar(s.getTelefone(), "Erro ao carregar o cardápio. Tente novamente em instantes.");
        }
    }

    private void processarCategoria(SessaoWhatsapp s, String texto) {
        if (texto.equals("ver carrinho") && !s.getItens().isEmpty()) {
            s.setEstado(EstadoSessao.REVISANDO_CARRINHO);
            mostrarCarrinho(s);
            return;
        }

        try {
            List<CatalogClient.CategoriaDto> categorias = catalogClient.listarCategorias(s.getRestauranteId())
                .stream().filter(CatalogClient.CategoriaDto::isAtivo).toList();

            int idx = Integer.parseInt(texto) - 1;
            if (idx < 0 || idx >= categorias.size()) throw new EntradaInvalidaException("Opção inválida. Escolha um número da lista:");

            CatalogClient.CategoriaDto cat = categorias.get(idx);
            s.setCategoriaSelecionadaId(cat.getId());
            s.setEstado(EstadoSessao.NAVEGANDO_PRODUTOS);
            mostrarProdutos(s, cat.getId(), cat.getNome());
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("Digite o *número* da categoria desejada:");
        }
    }

    private void mostrarProdutos(SessaoWhatsapp s, Long catId, String catNome) {
        try {
            List<CatalogClient.ProdutoDto> produtos = catalogClient.listarProdutos(s.getRestauranteId(), catId)
                .stream().filter(CatalogClient.ProdutoDto::isAtivo).toList();

            if (produtos.isEmpty()) {
                enviar(s.getTelefone(), "Sem produtos nesta categoria. Voltando ao menu...");
                s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
                mostrarCategorias(s);
                return;
            }

            StringBuilder sb = new StringBuilder("*" + catNome + "*\n\n");
            for (int i = 0; i < produtos.size(); i++) {
                CatalogClient.ProdutoDto p = produtos.get(i);
                sb.append(i + 1).append(". ").append(p.getNome())
                    .append(" — R$ ").append(String.format("%.2f", p.getPreco())).append("\n");
                if (p.getDescricao() != null && !p.getDescricao().isBlank()) {
                    sb.append("   _").append(p.getDescricao()).append("_\n");
                }
            }
            sb.append("\n0. ← Voltar às categorias");
            if (!s.getItens().isEmpty()) sb.append("\nC. Ver carrinho");
            enviar(s.getTelefone(), sb.toString());
        } catch (Exception e) {
            log.error("Erro ao buscar produtos (cat {}): {}", catId, e.getMessage(), e);
            enviar(s.getTelefone(), "Erro ao carregar produtos. Tente novamente.");
        }
    }

    private void processarProduto(SessaoWhatsapp s, String texto) {
        if (texto.equals("c") && !s.getItens().isEmpty()) {
            s.setEstado(EstadoSessao.REVISANDO_CARRINHO);
            mostrarCarrinho(s);
            return;
        }
        if (texto.equals("0")) {
            s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
            mostrarCategorias(s);
            return;
        }

        try {
            List<CatalogClient.ProdutoDto> produtos = catalogClient.listarProdutos(s.getRestauranteId(), s.getCategoriaSelecionadaId())
                .stream().filter(CatalogClient.ProdutoDto::isAtivo).toList();

            int idx = Integer.parseInt(texto) - 1;
            if (idx < 0 || idx >= produtos.size()) throw new EntradaInvalidaException("Opção inválida. Escolha um número do menu:");

            CatalogClient.ProdutoDto prod = produtos.get(idx);
            s.setProdutoSelecionadoId(prod.getId());
            s.setEstado(EstadoSessao.AGUARDANDO_QUANTIDADE);
            enviar(s.getTelefone(), "Quantas unidades de *" + prod.getNome() + "*?");
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("Digite o *número* do produto:");
        }
    }

    private void processarQuantidade(SessaoWhatsapp s, String texto) {
        try {
            int qty = Integer.parseInt(texto);
            if (qty < 1 || qty > 99) throw new EntradaInvalidaException("Quantidade inválida. Digite um número entre 1 e 99:");

            List<CatalogClient.ProdutoDto> produtos = catalogClient.listarProdutos(s.getRestauranteId(), s.getCategoriaSelecionadaId())
                .stream().filter(CatalogClient.ProdutoDto::isAtivo).toList();

            Optional<CatalogClient.ProdutoDto> prodOpt = produtos.stream()
                .filter(p -> p.getId().equals(s.getProdutoSelecionadoId())).findFirst();

            if (prodOpt.isEmpty()) {
                s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
                mostrarCategorias(s);
                return;
            }

            CatalogClient.ProdutoDto prod = prodOpt.get();
            Optional<ItemCarrinho> existente = s.getItens().stream()
                .filter(i -> i.getProdutoId().equals(prod.getId())).findFirst();

            if (existente.isPresent()) {
                existente.get().setQuantidade(existente.get().getQuantidade() + qty);
            } else {
                ItemCarrinho item = ItemCarrinho.builder()
                    .sessao(s).produtoId(prod.getId()).produtoNome(prod.getNome())
                    .precoUnitario(prod.getPreco()).quantidade(qty).build();
                s.getItens().add(item);
            }

            enviar(s.getTelefone(), "✅ Adicionado! " + qty + "x " + prod.getNome());
            s.setEstado(EstadoSessao.NAVEGANDO_PRODUTOS);
            mostrarProdutos(s, s.getCategoriaSelecionadaId(), "Continuar comprando");
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("Digite um número válido para a quantidade:");
        }
    }

    private void mostrarCarrinho(SessaoWhatsapp s) {
        if (s.getItens().isEmpty()) {
            s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
            enviar(s.getTelefone(), "Seu carrinho está vazio.");
            mostrarCategorias(s);
            return;
        }

        BigDecimal total = s.getItens().stream()
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder("🛒 *Seu Carrinho*\n\n");
        for (ItemCarrinho item : s.getItens()) {
            sb.append("• ").append(item.getQuantidade()).append("x ").append(item.getProdutoNome())
                .append(" — R$ ").append(String.format("%.2f", item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))).append("\n");
        }
        sb.append("\n*Total: R$ ").append(String.format("%.2f", total)).append("*\n\n");
        sb.append("1. ✅ Finalizar pedido\n");
        sb.append("2. ➕ Adicionar mais itens\n");
        sb.append("3. 🗑️ Limpar carrinho");
        enviar(s.getTelefone(), sb.toString());
    }

    private void processarRevisao(SessaoWhatsapp s, String texto) {
        switch (texto) {
            case "1" -> {
                s.setEstado(EstadoSessao.COLETANDO_ENDERECO);
                enviar(s.getTelefone(),
                    mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_ENDERECO", null));
            }
            case "2" -> {
                s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
                mostrarCategorias(s);
            }
            case "3" -> {
                s.getItens().clear();
                s.setEstado(EstadoSessao.NAVEGANDO_CATEGORIAS);
                enviar(s.getTelefone(), "Carrinho limpo!");
                mostrarCategorias(s);
            }
            default -> throw new EntradaInvalidaException("Digite 1, 2 ou 3:");
        }
    }

    private void processarPagamento(SessaoWhatsapp s, String texto) {
        String metodo = switch (texto) {
            case "1" -> "DINHEIRO";
            case "2" -> "PIX";
            case "3" -> "CARTAO_CREDITO";
            case "4" -> "CARTAO_DEBITO";
            default -> throw new EntradaInvalidaException("Escolha 1, 2, 3 ou 4:");
        };
        s.setMetodoPagamento(metodo);
        s.setEstado(EstadoSessao.COLETANDO_OBSERVACAO);
        enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_OBSERVACAO", null));
    }

    private void processarObservacao(SessaoWhatsapp s, String texto) {
        if (!texto.equals("não") && !texto.equals("nao") && !texto.equals("n")) {
            s.setObservacao(texto.trim());
        }
        s.setEstado(EstadoSessao.CONFIRMANDO_PEDIDO);
        mostrarResumoFinal(s);
    }

    private void mostrarResumoFinal(SessaoWhatsapp s) {
        BigDecimal total = s.getItens().stream()
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder("📋 *Resumo do Pedido*\n\n");
        sb.append("👤 *Cliente:* ").append(s.getClienteNome()).append("\n");
        sb.append("📍 *Endereço:* ").append(s.getEnderecoRua()).append("\n\n");
        sb.append("🛒 *Itens:*\n");
        for (ItemCarrinho item : s.getItens()) {
            sb.append("• ").append(item.getQuantidade()).append("x ").append(item.getProdutoNome()).append("\n");
        }
        sb.append("\n💳 *Pagamento:* ").append(formatarMetodo(s.getMetodoPagamento())).append("\n");
        if (s.getObservacao() != null) sb.append("📝 *Obs:* ").append(s.getObservacao()).append("\n");
        sb.append("\n*Total: R$ ").append(String.format("%.2f", total)).append("*\n\n");
        sb.append("1. ✅ Confirmar pedido\n2. ❌ Cancelar");
        enviar(s.getTelefone(), sb.toString());
    }

    private void processarConfirmacao(SessaoWhatsapp s, String texto) {
        if (texto.equals("1")) {
            enviarPedido(s);
        } else if (texto.equals("2")) {
            resetarSessao(s);
            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_CANCELADO", null));
        } else {
            throw new EntradaInvalidaException("Digite 1 para confirmar ou 2 para cancelar:");
        }
    }

    private void enviarPedido(SessaoWhatsapp s) {
        try {
            List<OrderClient.ItemEntregaRequest> itens = s.getItens().stream()
                .map(i -> OrderClient.ItemEntregaRequest.builder()
                    .produtoId(i.getProdutoId()).produtoNome(i.getProdutoNome())
                    .precoUnitario(i.getPrecoUnitario()).quantidade(i.getQuantidade())
                    .build())
                .toList();

            OrderClient.EntregaRequest request = OrderClient.EntregaRequest.builder()
                .clienteNome(s.getClienteNome())
                .clienteTelefone(s.getTelefone())
                .enderecoRua(s.getEnderecoRua())
                .enderecoNumero(null)
                .enderecoBairro(null)
                .enderecoCidade(null)
                .enderecoComplemento(null)
                .metodoPagamento(s.getMetodoPagamento())
                .observacao(s.getObservacao())
                .origemWhatsapp(true)
                .itens(itens)
                .build();

            OrderClient.EntregaResponse resp = orderClient.criarEntrega(s.getRestauranteId(), request);
            s.setEntregaId(resp.getId());
            s.setEstado("PIX".equals(s.getMetodoPagamento()) ? EstadoSessao.AGUARDANDO_PIX : EstadoSessao.PEDIDO_ENVIADO);

            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIDO_ENVIADO",
                Map.of("PEDIDO_ID", String.valueOf(resp.getId()))));
        } catch (Exception e) {
            log.error("Erro ao criar entrega via WhatsApp: {}", e.getMessage(), e);
            enviar(s.getTelefone(), "Erro ao enviar o pedido. Tente novamente ou ligue para nós.");
        }
    }

    public void notificarStatus(String telefone, String mensagem) {
        enviar(telefone, mensagem);
    }

    private void resetarSessao(SessaoWhatsapp s) {
        s.setEstado(EstadoSessao.INICIO);
        s.setClienteNome(null);
        s.setEnderecoRua(null);
        s.setEnderecoNumero(null);
        s.setEnderecoBairro(null);
        s.setEnderecoCidade(null);
        s.setEnderecoComplemento(null);
        s.setMetodoPagamento(null);
        s.setCategoriaSelecionadaId(null);
        s.setProdutoSelecionadoId(null);
        s.setObservacao(null);
        s.setEntregaId(null);
        s.setErrosConsecutivos(0);
        s.getItens().clear();
    }

    private void enviar(String telefone, String texto) {
        if (texto == null || texto.isBlank()) return; // mensagem desativada pelo admin
        evolutionClient.enviarMensagem(telefone, texto);
    }

    private String formatarMetodo(String metodo) {
        return switch (metodo) {
            case "DINHEIRO" -> "Dinheiro";
            case "PIX" -> "PIX";
            case "CARTAO_CREDITO" -> "Cartão de Crédito";
            case "CARTAO_DEBITO" -> "Cartão de Débito";
            default -> metodo;
        };
    }

    private static class EntradaInvalidaException extends RuntimeException {
        EntradaInvalidaException(String message) { super(message); }
    }
}
