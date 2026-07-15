package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.client.AuthClient;
import com.oiaaconta.whatsapp.client.EvolutionApiClient;
import com.oiaaconta.whatsapp.client.OrderClient;
import com.oiaaconta.whatsapp.entity.ItemCarrinho;
import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import com.oiaaconta.whatsapp.enums.EstadoSessao;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final SessaoWhatsappRepository sessaoRepo;
    private final EvolutionApiClient evolutionClient;
    private final OrderClient orderClient;
    private final MensagemTemplateService mensagemService;
    private final AuthClient authClient;
    private final MensagemWhatsappService mensagemWhatsappService;
    private final WhatsappConfigService whatsappConfigService;

    @Value("${app.frontend-base-url:http://localhost}")
    private String frontendBaseUrl;

    private static final int MAX_ERROS = 3;

    // Intencionalmente SEM @Transactional aqui: este método faz várias chamadas
    // HTTP bloqueantes (Feign para auth/order, RestTemplate para a Evolution
    // API) intercaladas com a lógica de estado da conversa, e não queremos
    // segurar uma conexão/transação JPA aberta durante esses round-trips de
    // rede. SessaoWhatsapp.itens é FetchType.EAGER, então a sessão (incluindo
    // itens do carrinho) já vem totalmente carregada do
    // findByTelefoneAndRestauranteId abaixo — ela pode ser lida/mutada em
    // memória enquanto "detached" sem disparar lazy-loading. Os pontos de
    // escrita (sessaoRepo.save(...)) permanecem, cada um cobrindo uma
    // transação curta e própria (Spring Data abre/fecha uma por chamada).
    @Async
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

        // Atualiza o nome de exibição com o nome de perfil do WhatsApp (pushName)
        // sempre que ele vier preenchido, independente do estado da conversa —
        // assim o painel mostra o nome do contato em vez do id "@lid" cru
        // mesmo antes do fluxo do chatbot chegar a perguntar o nome/número.
        // Salva na hora (não só no final do método) porque alguns caminhos
        // abaixo retornam cedo sem chegar ao sessaoRepo.save(sessao) final.
        if (pushName != null && !pushName.isBlank() && !pushName.trim().equals(sessao.getClienteNome())) {
            sessao.setClienteNome(pushName.trim());
            sessao = sessaoRepo.save(sessao);
        }

        // Captura TODA mensagem recebida, mesmo comandos como "cancelar"/"atendente"
        mensagemWhatsappService.registrar(restauranteId, telefone, DirecaoMensagem.RECEBIDA, texto);

        // Chatbot desativado pelo admin: a conexão com a Evolution API continua
        // ativa e a mensagem já foi registrada acima (aparece em Conversas),
        // mas o bot não responde automaticamente — atendimento fica manual.
        if (!whatsappConfigService.isChatbotAtivo(restauranteId)) return;

        // Sessão pausada — humano está respondendo; só "retomar bot" reativa
        if (sessao.getEstado() == EstadoSessao.PAUSADO) {
            if (textoLimpo.equals("retomar bot") || textoLimpo.equals("RETOMAR_BOT")) {
                resetarSessao(sessao);
                sessaoRepo.save(sessao);
                enviar(telefone, "Atendimento automatico reativado! Como posso te ajudar?", restauranteId);
            }
            return;
        }

        if (textoLimpo.equals("cancelar") || textoLimpo.equals("sair")) {
            resetarSessao(sessao);
            sessaoRepo.save(sessao);
            enviar(telefone, mensagemService.resolverTexto(restauranteId, "CHATBOT_CANCELADO", null), restauranteId);
            return;
        }

        // Pausa o bot e chama atendente humano
        if (textoLimpo.equals("atendente") || textoLimpo.equals("PAUSAR_BOT") || textoLimpo.equals("falar com atendente")) {
            sessao.setEstado(EstadoSessao.PAUSADO);
            sessaoRepo.save(sessao);
            enviar(telefone, "Ok! Um atendente ira responder em breve.\n\nQuando quiser retornar ao atendimento automatico, digite *retomar bot*.", restauranteId);
            return;
        }

        try {
            switch (sessao.getEstado()) {
                case INICIO -> processarInicio(sessao, pushName);
                case COLETANDO_NUMERO_LID -> processarNumeroLid(sessao, texto, pushName);
                case COLETANDO_NOME -> processarNome(sessao, texto);
                case COLETANDO_ENDERECO -> processarEndereco(sessao, texto);
                case AGUARDANDO_PEDIDO_WEB -> reenviarLinkCardapio(sessao);
                case COLETANDO_PAGAMENTO -> processarPagamento(sessao, textoLimpo);
                case COLETANDO_OBSERVACAO -> processarObservacao(sessao, textoLimpo);
                case CONFIRMANDO_PEDIDO -> processarConfirmacao(sessao, textoLimpo);
                case PEDIDO_ENVIADO, AGUARDANDO_PIX -> enviar(telefone,
                    mensagemService.resolverTexto(sessao.getRestauranteId(), "CHATBOT_JA_ENVIADO", null), restauranteId);
                default -> processarInicio(sessao, pushName);
            }
            sessao.setErrosConsecutivos(0);
        } catch (EntradaInvalidaException e) {
            int erros = sessao.getErrosConsecutivos() + 1;
            sessao.setErrosConsecutivos(erros);
            if (erros >= MAX_ERROS) {
                sessao.setErrosConsecutivos(0);
                enviar(telefone, "Não entendi sua resposta. " + e.getMessage() + "\n\nDigite *cancelar* a qualquer momento para recomeçar.", restauranteId);
            } else {
                enviar(telefone, e.getMessage(), restauranteId);
            }
        } catch (Exception e) {
            log.error("Erro no chatbot para {}: {}", telefone, e.getMessage(), e);
            enviar(telefone, "Ocorreu um erro. Tente novamente ou digite *cancelar* para recomeçar.", restauranteId);
        }

        sessaoRepo.save(sessao);
    }

    private boolean isLid(String telefone) {
        return telefone != null && telefone.endsWith("@lid");
    }

    private void processarInicio(SessaoWhatsapp s, String pushName) {
        if (isLid(s.getTelefone()) && s.getNumeroReal() == null) {
            resetarSessao(s);
            s.setEstado(EstadoSessao.COLETANDO_NUMERO_LID);
            enviar(s.getTelefone(),
                "Antes de começar, pra gente conseguir te atender melhor, qual o seu WhatsApp com DDD? (só números, ex: 11999998888)",
                s.getRestauranteId());
            return;
        }
        resetarSessao(s);
        if (pushName != null && !pushName.isBlank()) {
            s.setClienteNome(pushName.trim());
            String msg = mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_BOAS_VINDAS",
                Map.of("NOME", pushName.trim()));
            enviar(s.getTelefone(), msg, s.getRestauranteId());
            enviarLinkCardapio(s);
            enviarBotaoAtendente(s);
            s.setEstado(EstadoSessao.AGUARDANDO_PEDIDO_WEB);
        } else {
            s.setEstado(EstadoSessao.COLETANDO_NOME);
            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_NOME", null), s.getRestauranteId());
        }
    }

    private void enviarBotaoAtendente(SessaoWhatsapp s) {
        try {
            String titulo = "Precisa de ajuda?";
            String descricao = "Clique abaixo para falar com um atendente";
            evolutionClient.enviarBotaoResposta(
                s.getTelefone(),
                titulo,
                descricao,
                "Falar com atendente",
                "PAUSAR_BOT"
            );
            mensagemWhatsappService.registrar(s.getRestauranteId(), s.getTelefone(),
                DirecaoMensagem.ENVIADA, titulo + "\n" + descricao);
        } catch (Exception e) {
            log.warn("Nao foi possivel enviar botao de atendente: {}", e.getMessage());
        }
    }

    private void enviarLinkCardapio(SessaoWhatsapp s) {
        try {
            Map<String, String> slugMap = authClient.getSlug(s.getRestauranteId());
            String slug = slugMap.get("slug");
            if (slug == null || slug.isBlank()) return;

            String jid = s.getTelefone();
            String link = frontendBaseUrl + "/cardapio/" + slug;
            enviar(jid, link, s.getRestauranteId());
        } catch (Exception e) {
            log.warn("Não foi possível enviar link do cardápio: {}", e.getMessage());
        }
    }

    private void reenviarLinkCardapio(SessaoWhatsapp s) {
        enviar(s.getTelefone(), "Para fazer seu pedido, acesse o link do nosso cardápio digital:", s.getRestauranteId());
        enviarLinkCardapio(s);
    }

    private void processarNumeroLid(SessaoWhatsapp s, String texto, String pushName) {
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new EntradaInvalidaException(
                "Número inválido. Envie seu WhatsApp com DDD, só números (ex: 11999998888):");
        }
        s.setNumeroReal(digitos);
        processarInicio(s, pushName);
    }

    private void processarNome(SessaoWhatsapp s, String texto) {
        if (texto.trim().length() < 2) throw new EntradaInvalidaException("Nome muito curto. Informe seu nome:");
        s.setClienteNome(texto.trim());
        String msg = mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_SAUDACAO_APOS_NOME",
            Map.of("NOME", s.getClienteNome()));
        enviar(s.getTelefone(), msg, s.getRestauranteId());
        enviarLinkCardapio(s);
        enviarBotaoAtendente(s);
        s.setEstado(EstadoSessao.AGUARDANDO_PEDIDO_WEB);
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
        enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_PAGAMENTO", null), s.getRestauranteId());
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
        enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_OBSERVACAO", null), s.getRestauranteId());
    }

    private void processarObservacao(SessaoWhatsapp s, String texto) {
        if (!texto.equals("não") && !texto.equals("nao") && !texto.equals("n")) {
            s.setObservacao(texto.trim());
        }
        s.setEstado(EstadoSessao.CONFIRMANDO_PEDIDO);
        mostrarResumoFinal(s);
    }

    @SuppressWarnings("null")
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
        enviar(s.getTelefone(), sb.toString(), s.getRestauranteId());
    }

    private void processarConfirmacao(SessaoWhatsapp s, String texto) {
        if (texto.equals("1")) {
            enviarPedido(s);
        } else if (texto.equals("2")) {
            resetarSessao(s);
            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_CANCELADO", null), s.getRestauranteId());
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
                Map.of("PEDIDO_ID", String.valueOf(resp.getId()))), s.getRestauranteId());
        } catch (Exception e) {
            log.error("Erro ao criar entrega via WhatsApp: {}", e.getMessage(), e);
            enviar(s.getTelefone(), "Erro ao enviar o pedido. Tente novamente ou ligue para nós.", s.getRestauranteId());
        }
    }

    public void notificarStatus(String telefone, String mensagem, Long restauranteId) {
        enviar(telefone, mensagem, restauranteId);
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
        s.setObservacao(null);
        s.setEntregaId(null);
        s.setErrosConsecutivos(0);
        s.getItens().clear();
    }

    private void enviar(String telefone, String texto, Long restauranteId) {
        if (texto == null || texto.isBlank()) return; // mensagem desativada pelo admin
        evolutionClient.enviarMensagem(telefone, texto);
        mensagemWhatsappService.registrar(restauranteId, telefone, DirecaoMensagem.ENVIADA, texto);
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
