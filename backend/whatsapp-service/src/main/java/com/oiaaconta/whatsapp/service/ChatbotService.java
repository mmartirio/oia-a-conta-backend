package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.client.AuthClient;
import com.oiaaconta.whatsapp.client.CatalogClient;
import com.oiaaconta.whatsapp.client.EvolutionApiClient;
import com.oiaaconta.whatsapp.client.OllamaClient;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
    private final CatalogClient catalogClient;
    private final OllamaClient ollamaClient;

    @Value("${app.frontend-base-url:http://localhost}")
    private String frontendBaseUrl;

    private static final int MAX_ERROS = 3;

    // Mensagem "só números/vírgulas/espaços" (ex: "2, 5, 5") — via rápida sem
    // IA. Qualquer letra na mensagem cai no caminho com IA (Ollama).
    private static final Pattern SOMENTE_NUMEROS_SEPARADOS = Pattern.compile("^[\\d\\s,;]+$");

    // Trava por telefone+restaurante — evita a condição de corrida do
    // find-then-create de SessaoWhatsapp: sem isso, duas mensagens quase
    // simultâneas do mesmo número (cada uma num thread do pool @Async) podiam
    // achar "nenhuma sessão ainda" ao mesmo tempo e criar duas linhas
    // duplicadas (quebrando qualquer query que espere uma sessão única por
    // telefone, ex: listagem de conversas). Processar mensagens do MESMO
    // telefone em sequência também é o comportamento certo por si só — o
    // estado da conversa depende da ordem em que as mensagens chegam.
    // Números DIFERENTES continuam processando em paralelo normalmente (o
    // lock só serializa quem compartilha a mesma chave).
    private final ConcurrentHashMap<String, Object> locksPorTelefone = new ConcurrentHashMap<>();

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
    public void processarMensagem(String telefone, String texto, Long restauranteId, String pushName, String numeroRealAlt) {
        Object lock = locksPorTelefone.computeIfAbsent(telefone + '|' + restauranteId, k -> new Object());
        synchronized (lock) {
            processarMensagemSincronizado(telefone, texto, restauranteId, pushName, numeroRealAlt);
        }
    }

    @SuppressWarnings("null")
    private void processarMensagemSincronizado(String telefone, String texto, Long restauranteId, String pushName, String numeroRealAlt) {
        if (texto == null || texto.isBlank()) return;
        String textoLimpo = texto.trim().toLowerCase();

        SessaoWhatsapp sessao = sessaoRepo.findByTelefoneAndRestauranteId(telefone, restauranteId)
            .orElseGet(() -> SessaoWhatsapp.builder()
                .telefone(telefone)
                .restauranteId(restauranteId)
                .estado(EstadoSessao.INICIO)
                .build());

        // O WhatsApp já manda o número de verdade do contato "@lid" (ver
        // WebhookController) — evita perguntar de novo se já sabemos.
        boolean numeroRealNovo = numeroRealAlt != null && !numeroRealAlt.isBlank() && sessao.getNumeroReal() == null;
        if (numeroRealNovo) sessao.setNumeroReal(numeroRealAlt);

        // Atualiza o nome de exibição com o nome de perfil do WhatsApp (pushName)
        // sempre que ele vier preenchido, independente do estado da conversa —
        // assim o painel mostra o nome do contato em vez do id "@lid" cru
        // mesmo antes do fluxo do chatbot chegar a perguntar o nome/número.
        // Salva na hora (não só no final do método) porque alguns caminhos
        // abaixo retornam cedo sem chegar ao sessaoRepo.save(sessao) final.
        boolean nomeNovo = pushName != null && !pushName.isBlank() && !pushName.trim().equals(sessao.getClienteNome());
        if (nomeNovo) sessao.setClienteNome(pushName.trim());
        if (numeroRealNovo || nomeNovo) {
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

        // Loja fechada (fora do horário, pausa programada ou fechamento
        // manual) — responde avisando e encerra a conversa (reseta pra
        // INICIO) em vez de deixar a sessão parada num estado do fluxo de
        // pedido. Assim, a PRÓXIMA mensagem do cliente é tratada como um
        // contato novo e checa o status de novo — se reabriu, segue o fluxo
        // normal; se continua fechado, recebe o aviso de novo.
        OrderClient.StatusFuncionamentoResponse statusLoja = consultarStatusLoja(restauranteId);
        if (statusLoja != null && !statusLoja.isAberto()) {
            resetarSessao(sessao);
            sessaoRepo.save(sessao);
            enviar(telefone, mensagemService.resolverTexto(restauranteId, "CHATBOT_FECHADO",
                Map.of("MOTIVO", motivoFechado(statusLoja))), restauranteId);
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
                case AGUARDANDO_PEDIDO_WEB, COLETANDO_PEDIDO_CHAT -> processarPossivelPedido(sessao, texto, textoLimpo);
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
        String nomeRestaurante = buscarNomeRestaurante(s.getRestauranteId());
        if (pushName != null && !pushName.isBlank()) {
            s.setClienteNome(pushName.trim());
            String msg = mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_BOAS_VINDAS",
                Map.of("NOME", pushName.trim(), "RESTAURANTE", nomeRestaurante));
            enviar(s.getTelefone(), msg, s.getRestauranteId());
            enviarLinkCardapio(s);
            enviarBotaoAtendente(s);
            s.setEstado(EstadoSessao.AGUARDANDO_PEDIDO_WEB);
        } else {
            s.setEstado(EstadoSessao.COLETANDO_NOME);
            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_NOME",
                Map.of("RESTAURANTE", nomeRestaurante)), s.getRestauranteId());
        }
    }

    // Best-effort: se auth-service estiver fora do ar, cai num texto genérico
    // em vez de travar a saudação do chatbot por causa disso.
    private String buscarNomeRestaurante(Long restauranteId) {
        try {
            Map<String, String> info = authClient.getSlug(restauranteId);
            String nome = info != null ? info.get("nome") : null;
            return (nome != null && !nome.isBlank()) ? nome : "nosso delivery";
        } catch (Exception e) {
            log.warn("Falha ao buscar nome do restaurante {}: {}", restauranteId, e.getMessage());
            return "nosso delivery";
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

    // Cardápio público mudou de path (/cardapio/:slug) para subdomínio
    // (slug.dominio) — deriva o domínio a partir da própria frontend-base-url
    // (removendo esquema e um eventual "www.") em vez de outra env var, pra
    // não ter duas configs que podem ficar dessincronizadas.
    private String linkCardapio(String slug) {
        String host = frontendBaseUrl.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        return "https://" + slug + "." + host;
    }

    private void enviarLinkCardapio(SessaoWhatsapp s) {
        try {
            Map<String, String> slugMap = authClient.getSlug(s.getRestauranteId());
            String slug = slugMap.get("slug");
            if (slug == null || slug.isBlank()) return;

            String jid = s.getTelefone();
            String link = linkCardapio(slug);
            enviar(jid, link, s.getRestauranteId());
        } catch (Exception e) {
            log.warn("Não foi possível enviar link do cardápio: {}", e.getMessage());
        }
    }

    private void reenviarLinkCardapio(SessaoWhatsapp s) {
        enviar(s.getTelefone(), "Para fazer seu pedido, acesse o link do nosso cardápio digital:", s.getRestauranteId());
        enviarLinkCardapio(s);
    }

    // Ponto de entrada do pedido "em texto livre" — tenta entender a mensagem
    // como um pedido (números do cardápio, nome de produto, endereço e forma
    // de pagamento, misturados ou não) e avança o fluxo pra o que ainda falta,
    // reaproveitando os mesmos passos do fluxo legado (endereço → pagamento →
    // resumo/confirmação). Se não conseguir entender nada, cai de volta pro
    // link do cardápio (estado AGUARDANDO_PEDIDO_WEB) ou pede pra tentar de
    // novo com os números do cardápio (estado COLETANDO_PEDIDO_CHAT).
    private void processarPossivelPedido(SessaoWhatsapp s, String textoOriginal, String textoLimpo) {
        List<CatalogClient.ProdutoNumeradoResponse> catalogo = buscarCatalogoNumerado(s.getRestauranteId());

        boolean interpretou = false;

        if (!catalogo.isEmpty() && SOMENTE_NUMEROS_SEPARADOS.matcher(textoLimpo).matches()) {
            interpretou = tentarInterpretarNumeros(s, textoLimpo, catalogo);
        } else if (!catalogo.isEmpty() && textoOriginal.trim().length() >= 8) {
            // Mensagem com letras (nome de produto/endereço/pagamento
            // misturados) — só vale a pena chamar a IA se tiver conteúdo
            // (evita gastar uma chamada de inferência em "oi"/"ok").
            interpretou = tentarInterpretarComIA(s, textoOriginal, catalogo);
        }

        if (!interpretou) {
            if (s.getEstado() == EstadoSessao.COLETANDO_PEDIDO_CHAT) {
                enviar(s.getTelefone(), "Não entendi. Responda com os números dos produtos que deseja, separados por vírgula (ex: 1, 3, 3):\n\n"
                    + montarTextoCardapio(catalogo), s.getRestauranteId());
            } else {
                reenviarLinkCardapio(s);
            }
            return;
        }

        avancarAposColetarItens(s);
    }

    private List<CatalogClient.ProdutoNumeradoResponse> buscarCatalogoNumerado(Long restauranteId) {
        try {
            return catalogClient.listarProdutosNumerados(restauranteId);
        } catch (Exception e) {
            log.warn("Falha ao buscar cardápio numerado do restaurante {}: {}", restauranteId, e.getMessage());
            return List.of();
        }
    }

    private String montarTextoCardapio(List<CatalogClient.ProdutoNumeradoResponse> catalogo) {
        StringBuilder sb = new StringBuilder();
        for (CatalogClient.ProdutoNumeradoResponse p : catalogo) {
            sb.append(p.getNumero()).append(" - ").append(p.getNome())
                .append(" - R$ ").append(p.getPreco()).append("\n");
        }
        return sb.toString();
    }

    // "2, 5, 5" -> 1x produto do número 2, 2x produto do número 5 (conta as
    // repetições). Números que não batem com nenhum item do cardápio são
    // ignorados silenciosamente (o cliente vê o resumo final e pode corrigir).
    private boolean tentarInterpretarNumeros(SessaoWhatsapp s, String texto, List<CatalogClient.ProdutoNumeradoResponse> catalogo) {
        Map<Integer, Long> contagem = new java.util.LinkedHashMap<>();
        for (String parte : texto.split("[,;\\s]+")) {
            if (parte.isBlank()) continue;
            try {
                int numero = Integer.parseInt(parte.trim());
                contagem.merge(numero, 1L, Long::sum);
            } catch (NumberFormatException ignored) { }
        }
        if (contagem.isEmpty()) return false;

        boolean algumItem = false;
        for (Map.Entry<Integer, Long> entrada : contagem.entrySet()) {
            CatalogClient.ProdutoNumeradoResponse produto = catalogo.stream()
                .filter(p -> p.getNumero().equals(entrada.getKey()))
                .findFirst().orElse(null);
            if (produto == null) continue;
            adicionarOuIncrementarItem(s, produto.getProdutoId(), produto.getNome(), produto.getPreco(), entrada.getValue().intValue());
            algumItem = true;
        }
        return algumItem;
    }

    private boolean tentarInterpretarComIA(SessaoWhatsapp s, String textoOriginal, List<CatalogClient.ProdutoNumeradoResponse> catalogo) {
        OllamaClient.PedidoInterpretado interpretado = ollamaClient.interpretar(
            textoOriginal, catalogo, s.getEnderecoRua(), s.getMetodoPagamento());
        if (interpretado == null) return false;

        boolean algumaCoisa = false;

        if (interpretado.getItens() != null) {
            for (OllamaClient.ItemInterpretado item : interpretado.getItens()) {
                if (item.getProdutoId() == null) continue;
                CatalogClient.ProdutoNumeradoResponse produto = catalogo.stream()
                    .filter(p -> p.getProdutoId().equals(item.getProdutoId()))
                    .findFirst().orElse(null);
                if (produto == null) continue;
                int quantidade = item.getQuantidade() == null || item.getQuantidade() < 1 ? 1 : item.getQuantidade();
                adicionarOuIncrementarItem(s, produto.getProdutoId(), produto.getNome(), produto.getPreco(), quantidade);
                algumaCoisa = true;
            }
        }
        if (interpretado.getEndereco() != null && !interpretado.getEndereco().isBlank() && s.getEnderecoRua() == null) {
            s.setEnderecoRua(interpretado.getEndereco().trim());
            algumaCoisa = true;
        }
        if (interpretado.getFormaPagamento() != null && s.getMetodoPagamento() == null
            && List.of("DINHEIRO", "PIX", "CARTAO_CREDITO", "CARTAO_DEBITO").contains(interpretado.getFormaPagamento())) {
            s.setMetodoPagamento(interpretado.getFormaPagamento());
            algumaCoisa = true;
        }
        return algumaCoisa;
    }

    private void adicionarOuIncrementarItem(SessaoWhatsapp s, Long produtoId, String nome, BigDecimal preco, int quantidade) {
        ItemCarrinho existente = s.getItens().stream()
            .filter(i -> produtoId.equals(i.getProdutoId()))
            .findFirst().orElse(null);
        if (existente != null) {
            existente.setQuantidade(existente.getQuantidade() + quantidade);
            return;
        }
        if (s.getItens() == null) s.setItens(new ArrayList<>());
        s.getItens().add(ItemCarrinho.builder()
            .sessao(s).produtoId(produtoId).produtoNome(nome)
            .precoUnitario(preco).quantidade(quantidade)
            .build());
    }

    // Depois de mesclar o que foi entendido da mensagem, decide o próximo
    // passo exatamente como o fluxo legado (endereço → pagamento → resumo) —
    // pula direto pro resumo se endereço e pagamento já vieram na mesma
    // mensagem (ver item 3: cliente pode mandar tudo junto).
    private void avancarAposColetarItens(SessaoWhatsapp s) {
        if (s.getEnderecoRua() == null) {
            s.setEstado(EstadoSessao.COLETANDO_ENDERECO);
            enviar(s.getTelefone(), "Show! Anotado. Agora me informa o endereço de entrega:\n_Ex: Rua das Flores, 123, Centro, Aracaju_", s.getRestauranteId());
            return;
        }
        if (s.getMetodoPagamento() == null) {
            s.setEstado(EstadoSessao.COLETANDO_PAGAMENTO);
            enviar(s.getTelefone(), mensagemService.resolverTexto(s.getRestauranteId(), "CHATBOT_PEDIR_PAGAMENTO", null), s.getRestauranteId());
            return;
        }
        s.setEstado(EstadoSessao.CONFIRMANDO_PEDIDO);
        mostrarResumoFinal(s);
    }

    private void processarNumeroLid(SessaoWhatsapp s, String texto, String pushName) {
        // O número já pode ter sido identificado automaticamente (via
        // remoteJidAlt, ver processarMensagemSincronizado) numa mensagem
        // mais recente que a que colocou a sessão nesse estado — nesse caso
        // segue o fluxo normal em vez de tentar reinterpretar o texto atual
        // (que pode ser qualquer mensagem do cliente) como número digitado.
        if (s.getNumeroReal() != null) {
            processarInicio(s, pushName);
            return;
        }
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

        StringBuilder sb = new StringBuilder("Claro! Aqui está seu pedido!\nPor favor confirme abaixo para podermos enviar o pedido para a cozinha\n\n");
        sb.append("📋 *Resumo do Pedido*\n\n");
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

    private static final int MINUTOS_LEMBRETE_CARDAPIO = 10;

    // Varredura periódica (não precisa ser exata ao minuto — fixedDelay conta
    // a partir do fim da execução anterior, então não empilha se uma
    // varredura demorar). Sem @Transactional de propósito, mesmo motivo do
    // processarMensagem: cada sessão aqui dispara chamadas HTTP bloqueantes
    // (catalog-service via Feign, Evolution API via RestTemplate) — não dá
    // pra segurar uma transação JPA aberta durante isso.
    @Scheduled(fixedDelay = 60_000)
    public void enviarLembretesCardapio() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_LEMBRETE_CARDAPIO);
        List<SessaoWhatsapp> pendentes = sessaoRepo.findByEstadoAndLembreteCardapioEnviadoFalseAndUltimaInteracaoBefore(
            EstadoSessao.AGUARDANDO_PEDIDO_WEB, limite);
        for (SessaoWhatsapp s : pendentes) {
            try {
                enviarLembreteCardapio(s);
            } catch (Exception e) {
                log.error("Falha ao enviar lembrete de cardápio pra sessão {}: {}", s.getId(), e.getMessage());
            }
        }
    }

    private void enviarLembreteCardapio(SessaoWhatsapp s) {
        List<CatalogClient.ProdutoNumeradoResponse> catalogo = buscarCatalogoNumerado(s.getRestauranteId());
        String imagem = whatsappConfigService.getImagemCardapio(s.getRestauranteId());

        String textoCardapio = catalogo.isEmpty() ? "" : "\n\n" + montarTextoCardapio(catalogo);
        String instrucao = "Ainda está por aí? 😊 Segue nosso cardápio! Responda com os números dos produtos que deseja, separados por vírgula (ex: 1, 3, 3), ou escreva o que você quer, seu endereço e a forma de pagamento."
            + textoCardapio;

        if (imagem != null && !imagem.isBlank()) {
            evolutionClient.enviarImagem(s.getTelefone(), imagem, "");
            mensagemWhatsappService.registrar(s.getRestauranteId(), s.getTelefone(), DirecaoMensagem.ENVIADA, "[imagem do cardápio]");
        }
        enviar(s.getTelefone(), instrucao, s.getRestauranteId());

        s.setEstado(EstadoSessao.COLETANDO_PEDIDO_CHAT);
        s.setLembreteCardapioEnviado(true);
        sessaoRepo.save(s);
    }

    // Best-effort: se order-service estiver fora do ar ou a chamada falhar,
    // não bloqueia o atendimento — trata como se estivesse aberto (null),
    // igual à postura das outras integrações externas deste serviço.
    private OrderClient.StatusFuncionamentoResponse consultarStatusLoja(Long restauranteId) {
        try {
            return orderClient.statusFuncionamento(restauranteId);
        } catch (Exception e) {
            log.warn("Falha ao consultar status de funcionamento do restaurante {}: {}", restauranteId, e.getMessage());
            return null;
        }
    }

    private String motivoFechado(OrderClient.StatusFuncionamentoResponse status) {
        String motivo = status.getMotivo() != null && !status.getMotivo().isBlank()
            ? status.getMotivo()
            : "No momento não estamos atendendo.";
        if (status.getReaberturaPrevista() != null && !status.getReaberturaPrevista().isBlank()) {
            try {
                LocalDateTime reabertura = LocalDateTime.parse(status.getReaberturaPrevista());
                motivo += " Reabrimos " + reabertura.format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")) + ".";
            } catch (Exception ignored) {
                // formato inesperado — mostra só o motivo, sem a data de reabertura
            }
        }
        return motivo;
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
        s.setLembreteCardapioEnviado(false);
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
