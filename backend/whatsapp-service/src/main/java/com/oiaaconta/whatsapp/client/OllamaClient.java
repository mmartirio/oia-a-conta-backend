package com.oiaaconta.whatsapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oiaaconta.whatsapp.config.RestTemplateConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// IA local (Ollama, gratuita e open-source — ver docker-compose.yml) usada só
// pra interpretar texto livre do cliente no WhatsApp (nome de produto,
// endereço e forma de pagamento misturados numa mensagem só) — a via rápida
// sem IA (números separados por vírgula) é resolvida direto em ChatbotService
// via regex, sem passar por aqui.
@Component
@Slf4j
public class OllamaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.model}")
    private String model;

    // Extrai o primeiro objeto JSON top-level da resposta — o modelo às vezes
    // devolve texto antes/depois mesmo pedindo só o JSON, principalmente sem
    // o "format":"json" (removido de propósito: essa opção do Ollama força
    // decodificação restrita por gramática, que na prática deixou a geração
    // MUITO mais lenta nesse hardware — passava dos 45s até com o modelo 3b —
    // e ainda assim não garantia melhor aderência ao formato).
    private static final Pattern JSON_OBJETO = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    public OllamaClient(@RestTemplateConfig.Ollama RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Retorna null se o modelo não respondeu, deu timeout, ou devolveu algo
    // que não é o JSON esperado — call-sites devem tratar null como "não deu
    // pra entender, pede pro cliente responder com os números do cardápio".
    public PedidoInterpretado interpretar(String mensagemCliente, List<CatalogClient.ProdutoNumeradoResponse> catalogo,
                                           String enderecoAtual, String pagamentoAtual) {
        try {
            String prompt = montarPrompt(mensagemCliente, catalogo, enderecoAtual, pagamentoAtual);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                // num_predict baixo: a resposta esperada é só um JSON curto —
                // limitar corta geração desnecessária e acelera bastante.
                "options", Map.of("num_predict", 200, "num_ctx", 2048, "temperature", 0.1)
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> resposta = restTemplate.postForObject(apiUrl + "/api/generate", entity, Map.class);
            if (resposta == null || resposta.get("response") == null) return null;

            String bruto = resposta.get("response").toString();
            Matcher m = JSON_OBJETO.matcher(bruto);
            if (!m.find()) {
                log.warn("Resposta do Ollama sem JSON reconhecível: {}", bruto);
                return null;
            }
            PedidoInterpretado interpretado = objectMapper.readValue(m.group(), PedidoInterpretado.class);
            log.debug("Ollama interpretou '{}' como: {}", mensagemCliente, interpretado);
            return interpretado;
        } catch (Exception e) {
            log.warn("Falha ao interpretar mensagem via Ollama: {}", e.getMessage());
            return null;
        }
    }

    private String montarPrompt(String mensagemCliente, List<CatalogClient.ProdutoNumeradoResponse> catalogo,
                                 String enderecoAtual, String pagamentoAtual) {
        String cardapioTexto = catalogo.stream()
            .map(p -> p.getNumero() + " - " + p.getNome() + " (produtoId=" + p.getProdutoId() + ") - R$ " + p.getPreco())
            .collect(Collectors.joining("\n"));

        return """
            Você é um assistente que interpreta pedidos de clientes de um restaurante via WhatsApp.

            Cardápio disponível (número - nome (produtoId) - preço):
            %s

            Endereço de entrega já informado antes (pode ser null): %s
            Forma de pagamento já informada antes (pode ser null): %s

            Mensagem do cliente agora: "%s"

            Extraia dessa mensagem, se houver: itens do pedido (pelo número do cardápio OU pelo nome do produto — resolva sempre para o produtoId real listado acima), endereço de entrega, e forma de pagamento (deve ser exatamente DINHEIRO, PIX, CARTAO_CREDITO ou CARTAO_DEBITO).

            IMPORTANTE: todo item DEVE ter o campo "quantidade" preenchido com um número — nunca omita esse campo. Exemplo de mensagem parecida: "quero 3 coxinhas e uma coca, rua X 100, dinheiro" deve virar exatamente:
            {"itens":[{"produtoId":10,"quantidade":3},{"produtoId":20,"quantidade":1}],"endereco":"rua X 100","formaPagamento":"DINHEIRO"}
            (produtoId de exemplo — use os IDs reais do cardápio acima. Repare que "3 coxinhas" virou quantidade:3 e "uma coca" virou quantidade:1 — nunca deixe quantidade vazio ou null.)

            Responda APENAS com um JSON válido, sem nenhum texto antes ou depois, neste formato exato:
            {"itens":[{"produtoId":123,"quantidade":1}],"endereco":"endereço completo ou null","formaPagamento":"DINHEIRO|PIX|CARTAO_CREDITO|CARTAO_DEBITO ou null","itensNaoReconhecidos":["trecho da mensagem"]}

            Regras: só inclua em "itens" produtos que você reconheceu com confiança no cardápio acima; "quantidade" é sempre obrigatório (use 1 se a mensagem não deixar claro quantas unidades); se não identificar endereço ou forma de pagamento nessa mensagem, use null nesses campos (não repita o que já foi informado antes).

            "itensNaoReconhecidos": se a mensagem parecer estar pedindo um produto mas o nome usado for genérico ou ambíguo demais pra resolver com confiança pra um produtoId específico (ex: cliente pediu "um refrigerante" e o cardápio só tem "Coca-cola lata" e "Guaraná lata" — nenhum se chama literalmente "refrigerante"), inclua APENAS a palavra ou expressão ambígua nessa lista — nunca a mensagem inteira. Se não houver nenhum caso assim, use uma lista vazia [].
            Exemplo (cardápio só com "Coca-cola lata" e "Guaraná lata" como bebidas): mensagem "quero 1 carne e 1 refrigerante" deve virar:
            {"itens":[{"produtoId":2,"quantidade":1}],"endereco":null,"formaPagamento":null,"itensNaoReconhecidos":["refrigerante"]}
            (repare que "itensNaoReconhecidos" tem só a palavra "refrigerante", não a frase toda — o item "carne" foi resolvido normalmente e NÃO entra nessa lista.)
            """.formatted(cardapioTexto, enderecoAtual, pagamentoAtual, mensagemCliente);
    }

    @Data
    public static class PedidoInterpretado {
        private List<ItemInterpretado> itens;
        private String endereco;
        private String formaPagamento;
        private List<String> itensNaoReconhecidos;
    }

    @Data
    public static class ItemInterpretado {
        private Long produtoId;
        private Integer quantidade;
    }
}
