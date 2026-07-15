package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.dto.MensagemTemplateDto;
import com.oiaaconta.whatsapp.entity.MensagemTemplate;
import com.oiaaconta.whatsapp.repository.MensagemTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MensagemTemplateService {

    private final MensagemTemplateRepository repository;

    private static final Map<String, String> GRUPOS = new LinkedHashMap<>() {{
        put("CHATBOT_BOAS_VINDAS",          "chatbot");
        put("CHATBOT_PEDIR_NOME",            "chatbot");
        put("CHATBOT_SAUDACAO_APOS_NOME",    "chatbot");
        put("CHATBOT_PEDIR_ENDERECO",        "chatbot");
        put("CHATBOT_PEDIR_PAGAMENTO",       "chatbot");
        put("CHATBOT_PEDIR_OBSERVACAO",      "chatbot");
        put("CHATBOT_PEDIDO_ENVIADO",        "chatbot");
        put("CHATBOT_CANCELADO",             "chatbot");
        put("CHATBOT_JA_ENVIADO",            "chatbot");
        put("PEDIDO_ACEITO",                 "notificacao");
        put("PEDIDO_PRONTO",                 "notificacao");
        put("PEDIDO_SAIU",                   "notificacao");
        put("PEDIDO_ENTREGUE",               "notificacao");
        put("PEDIDO_PIX",                    "notificacao");
        put("PEDIDO_CANCELADO",              "notificacao");
        put("PEDIDO_REJEITADO",              "notificacao");
    }};

    private static final Map<String, String> LABELS = new LinkedHashMap<>() {{
        put("CHATBOT_BOAS_VINDAS",          "Boas-vindas (com nome do perfil)");
        put("CHATBOT_PEDIR_NOME",            "Pedir nome ao cliente");
        put("CHATBOT_SAUDACAO_APOS_NOME",    "Saudação após digitar o nome");
        put("CHATBOT_PEDIR_ENDERECO",        "Solicitar endereço de entrega");
        put("CHATBOT_PEDIR_PAGAMENTO",       "Solicitar forma de pagamento");
        put("CHATBOT_PEDIR_OBSERVACAO",      "Solicitar observações");
        put("CHATBOT_PEDIDO_ENVIADO",        "Pedido enviado com sucesso");
        put("CHATBOT_CANCELADO",             "Pedido cancelado pelo cliente");
        put("CHATBOT_JA_ENVIADO",            "Pedido já foi enviado (novo contato)");
        put("PEDIDO_ACEITO",                 "Pedido Aceito");
        put("PEDIDO_PRONTO",                 "Pedido Pronto para Entrega");
        put("PEDIDO_SAIU",                   "Pedido Saiu para Entrega");
        put("PEDIDO_ENTREGUE",               "Pedido Entregue");
        put("PEDIDO_PIX",                    "Pagamento via PIX");
        put("PEDIDO_CANCELADO",              "Pedido Cancelado");
        put("PEDIDO_REJEITADO",              "Pedido Recusado pelo Restaurante");
    }};

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>() {{
        put("CHATBOT_BOAS_VINDAS",
            "Olá, *{NOME}*! 🍕 Bem-vindo ao delivery!\n\nAqui está nosso cardápio:");
        put("CHATBOT_PEDIR_NOME",
            "Olá! 🍕 Bem-vindo ao delivery!\n\nQual é o seu nome?");
        put("CHATBOT_SAUDACAO_APOS_NOME",
            "Olá, *{NOME}*! 🍕\n\nAqui está nosso cardápio:");
        put("CHATBOT_PEDIR_ENDERECO",
            "📍 *Endereço de entrega*\n\nDigite seu endereço completo em uma mensagem:\n_Ex: Rua das Flores, 123, Centro, Aracaju_");
        put("CHATBOT_PEDIR_PAGAMENTO",
            "💳 *Forma de pagamento:*\n\n1. Dinheiro\n2. PIX\n3. Cartão de Crédito\n4. Cartão de Débito");
        put("CHATBOT_PEDIR_OBSERVACAO",
            "Alguma *observação* para o pedido? (ex: sem cebola)\nOu digite *não* para pular.");
        put("CHATBOT_PEDIDO_ENVIADO",
            "✅ *Pedido enviado com sucesso!* 🎉\n\nSeu pedido #{PEDIDO_ID} foi recebido e está sendo processado. Você receberá atualizações aqui!");
        put("CHATBOT_CANCELADO",
            "Pedido cancelado. Digite qualquer coisa para iniciar um novo pedido.");
        put("CHATBOT_JA_ENVIADO",
            "Seu pedido já foi enviado! Aguarde as atualizações. Digite *cancelar* para um novo pedido.");
        put("PEDIDO_ACEITO",    "✅ Seu pedido foi aceito! Estamos preparando tudo para você. 🍳");
        put("PEDIDO_PRONTO",    "🎉 Seu pedido está pronto! O entregador vai buscá-lo em breve.");
        put("PEDIDO_SAIU",      "🛵 Seu pedido saiu para entrega! O entregador está a caminho.");
        put("PEDIDO_ENTREGUE",  "✅ Pedido entregue! Obrigado pela preferência. 😊");
        put("PEDIDO_PIX",       "✅ Pedido entregue! Para finalizar, realize o pagamento via PIX.\n\n🔑 *Chave PIX:* {PIX_CHAVE}\n💰 *Valor:* R$ {VALOR}");
        put("PEDIDO_CANCELADO", "❌ Seu pedido foi cancelado. Entre em contato conosco para mais informações.");
        put("PEDIDO_REJEITADO", "❌ Não conseguimos aceitar seu pedido no momento.\n\n*Motivo:* {MOTIVO}\n\nPor favor, entre em contato conosco ou tente novamente mais tarde.");
    }};

    private static final Map<String, String> VARIAVEL_HINTS = new LinkedHashMap<>() {{
        put("CHATBOT_BOAS_VINDAS",       "Variável disponível: {NOME}");
        put("CHATBOT_SAUDACAO_APOS_NOME","Variável disponível: {NOME}");
        put("CHATBOT_PEDIDO_ENVIADO",    "Variável disponível: {PEDIDO_ID}");
        put("PEDIDO_PIX",                "Variáveis disponíveis: {PIX_CHAVE}, {VALOR}");
        put("PEDIDO_REJEITADO",          "Variável disponível: {MOTIVO}");
    }};

    private static final Map<String, Integer> ORDENS_PADRAO = new LinkedHashMap<>() {{
        put("CHATBOT_BOAS_VINDAS",       10);
        put("CHATBOT_PEDIR_NOME",        20);
        put("CHATBOT_SAUDACAO_APOS_NOME",30);
        put("CHATBOT_PEDIR_ENDERECO",    40);
        put("CHATBOT_PEDIR_PAGAMENTO",   50);
        put("CHATBOT_PEDIR_OBSERVACAO",  60);
        put("CHATBOT_PEDIDO_ENVIADO",    70);
        put("CHATBOT_CANCELADO",         80);
        put("CHATBOT_JA_ENVIADO",        90);
        put("PEDIDO_ACEITO",             10);
        put("PEDIDO_PRONTO",             20);
        put("PEDIDO_SAIU",               30);
        put("PEDIDO_ENTREGUE",           40);
        put("PEDIDO_PIX",                50);
        put("PEDIDO_CANCELADO",          60);
        put("PEDIDO_REJEITADO",          70);
    }};

    @SuppressWarnings("null")
    public String resolverTexto(Long restauranteId, String tipo, Map<String, String> vars) {
        var dbEntry = repository.findByRestauranteIdAndChave(restauranteId, tipo);
        // Mensagem desativada → retorna string vazia para o ChatbotService ignorar
        if (dbEntry.isPresent() && Boolean.FALSE.equals(dbEntry.get().getAtivo())) {
            return "";
        }
        String texto = dbEntry.map(MensagemTemplate::getTexto)
            .orElse(DEFAULTS.getOrDefault(tipo, tipo));

        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                texto = texto.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return texto;
    }

    @SuppressWarnings("null")
    public List<MensagemTemplateDto> listar(Long restauranteId) {
        List<MensagemTemplate> dbEntries = repository.findByRestauranteId(restauranteId);
        Map<String, MensagemTemplate> byChave = dbEntries.stream()
            .collect(Collectors.toMap(MensagemTemplate::getChave, t -> t));

        List<MensagemTemplateDto> result = new ArrayList<>();

        // 1. Mensagens de sistema (definidas em LABELS)
        for (Map.Entry<String, String> e : LABELS.entrySet()) {
            String chave = e.getKey();
            MensagemTemplate custom = byChave.get(chave);
            String padrao = DEFAULTS.get(chave);
            int ordemFinal = (custom != null && custom.getOrdem() != null)
                ? custom.getOrdem()
                : ORDENS_PADRAO.getOrDefault(chave, 99);
            String textoFinal = (custom != null) ? custom.getTexto() : padrao;
            boolean personalizado = custom != null && !Objects.equals(custom.getTexto(), padrao);
            boolean ativo = custom == null || !Boolean.FALSE.equals(custom.getAtivo());
            result.add(MensagemTemplateDto.builder()
                .chave(chave)
                .label(e.getValue())
                .texto(textoFinal)
                .textoPadrao(padrao)
                .personalizado(personalizado)
                .sistema(true)
                .ativo(ativo)
                .grupo(GRUPOS.getOrDefault(chave, "notificacao"))
                .ordem(ordemFinal)
                .variavelHint(VARIAVEL_HINTS.get(chave))
                .build());
        }

        // 2. Mensagens criadas pelo usuário (chave não está em LABELS)
        for (MensagemTemplate t : dbEntries) {
            if (!LABELS.containsKey(t.getChave())) {
                result.add(MensagemTemplateDto.builder()
                    .chave(t.getChave())
                    .label(t.getLabel() != null ? t.getLabel() : t.getChave())
                    .texto(t.getTexto())
                    .textoPadrao(null)
                    .personalizado(true)
                    .sistema(false)
                    .ativo(!Boolean.FALSE.equals(t.getAtivo()))
                    .grupo(t.getGrupo() != null ? t.getGrupo() : "notificacao")
                    .ordem(t.getOrdem() != null ? t.getOrdem() : 999)
                    .variavelHint(null)
                    .build());
            }
        }

        return result.stream()
            .sorted(Comparator.comparing(MensagemTemplateDto::getGrupo)
                .thenComparingInt(MensagemTemplateDto::getOrdem))
            .collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("null")
    public MensagemTemplateDto criar(Long restauranteId, String label, String texto, String grupo) {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Label obrigatório");
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Texto obrigatório");
        String grupoFinal = (grupo != null && !grupo.isBlank()) ? grupo : "notificacao";

        String chave = "CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        int maxOrdem = repository.findByRestauranteId(restauranteId).stream()
            .filter(t -> grupoFinal.equals(t.getGrupo()))
            .mapToInt(t -> t.getOrdem() != null ? t.getOrdem() : 0)
            .max().orElse(900) + 10;

        MensagemTemplate t = MensagemTemplate.builder()
            .restauranteId(restauranteId)
            .chave(chave)
            .label(label.trim())
            .texto(texto.trim())
            .grupo(grupoFinal)
            .ordem(maxOrdem)
            .build();
        repository.save(t);

        return MensagemTemplateDto.builder()
            .chave(chave).label(label.trim()).texto(texto.trim())
            .textoPadrao(null).personalizado(true).sistema(false)
            .grupo(grupoFinal).ordem(maxOrdem).build();
    }

    @Transactional
    public void salvar(Long restauranteId, String chave, String texto) {
        MensagemTemplate t = repository.findByRestauranteIdAndChave(restauranteId, chave)
            .orElse(MensagemTemplate.builder()
                .restauranteId(restauranteId)
                .chave(chave)
                .ordem(ORDENS_PADRAO.get(chave))
                .build());
        t.setTexto(texto);
        repository.save(t);
    }

    @Transactional
    public void salvarOrdem(Long restauranteId, List<Map<String, Object>> itens) {
        for (Map<String, Object> item : itens) {
            String chave = (String) item.get("chave");
            Integer ordem = item.get("ordem") instanceof Number n ? n.intValue() : null;
            if (chave == null || ordem == null) continue;

            MensagemTemplate t = repository.findByRestauranteIdAndChave(restauranteId, chave)
                .orElse(MensagemTemplate.builder()
                    .restauranteId(restauranteId)
                    .chave(chave)
                    .texto(DEFAULTS.getOrDefault(chave, ""))
                    .build());
            t.setOrdem(ordem);
            repository.save(t);
        }
    }

    @Transactional
    public void restaurarOuExcluir(Long restauranteId, String chave) {
        if (LABELS.containsKey(chave)) {
            // Mensagem de sistema: restaura o texto padrão e reativa
            MensagemTemplate t = repository.findByRestauranteIdAndChave(restauranteId, chave)
                .orElse(MensagemTemplate.builder()
                    .restauranteId(restauranteId).chave(chave).build());
            t.setTexto(DEFAULTS.getOrDefault(chave, ""));
            t.setOrdem(ORDENS_PADRAO.getOrDefault(chave, 99));
            t.setAtivo(true);
            repository.save(t);
        } else {
            // Mensagem customizada: exclui do banco
            repository.findByRestauranteIdAndChave(restauranteId, chave)
                .ifPresent(repository::delete);
        }
    }

    @Transactional
    public void remover(Long restauranteId, String chave) {
        if (LABELS.containsKey(chave)) {
            // Mensagem de sistema: marca como desativada (mantém texto, muda ativo=false)
            MensagemTemplate t = repository.findByRestauranteIdAndChave(restauranteId, chave)
                .orElse(MensagemTemplate.builder()
                    .restauranteId(restauranteId).chave(chave)
                    .texto(DEFAULTS.getOrDefault(chave, ""))
                    .ordem(ORDENS_PADRAO.getOrDefault(chave, 99))
                    .build());
            t.setAtivo(false);
            repository.save(t);
        } else {
            // Mensagem customizada: exclui do banco
            repository.findByRestauranteIdAndChave(restauranteId, chave)
                .ifPresent(repository::delete);
        }
    }
}
