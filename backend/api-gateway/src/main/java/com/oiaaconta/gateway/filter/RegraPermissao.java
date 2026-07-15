package com.oiaaconta.gateway.filter;

import java.util.List;
import java.util.Set;

// Mapa rota → permissão pros itens do sidebar que são isoláveis por prefixo
// de URL (ver plano em C:\Users\Martirio\.claude\plans\distributed-sleeping-umbrella.md
// — Cozinha/Comanda/Garçom/Delivery/Entregador/Caixa NÃO entram aqui porque
// convergem nos mesmos controllers de order-service; esses ficam só com o
// grant de role da Part A). Verificadas em ordem — a primeira regra cujo
// prefixo bate é a que vale, por isso prefixos mais específicos vêm antes
// dos mais genéricos (ex: /api/auth/restaurante/logo antes de
// /api/auth/restaurante).
public final class RegraPermissao {

    private RegraPermissao() { }

    public record Regra(String permissao, String prefixo, Set<String> metodos) { }

    private static final Set<String> ESCRITA = Set.of("POST", "PUT", "DELETE", "PATCH");

    // Prefixos que nunca passam pela checagem de permissão de grupo aqui —
    // continuam protegidos só pelo role (Part A) / @PreAuthorize downstream.
    // /api/financeiro/billing é do billing-service (relatório do Gestor/plataforma),
    // fora do escopo de Grupo por restaurante.
    public static final List<String> IGNORAR = List.of(
        "/api/financeiro/billing"
    );

    public static final List<Regra> REGRAS = List.of(
        new Regra("WHATSAPP_MENSAGENS", "/api/whatsapp/admin/mensagens", null),
        new Regra("WHATSAPP_CONVERSAS", "/api/whatsapp/admin/conversas", null),
        new Regra("WHATSAPP_CONEXAO", "/api/whatsapp/admin/status", null),
        new Regra("WHATSAPP_CONEXAO", "/api/whatsapp/admin/conectar", null),
        new Regra("WHATSAPP_CONEXAO", "/api/whatsapp/admin/desconectar", null),
        new Regra("WHATSAPP_CONEXAO", "/api/whatsapp/admin/chatbot-status", null),

        new Regra("CONFIG_LOGO", "/api/auth/restaurante/logo", null),
        new Regra("CONFIG_CORES", "/api/auth/restaurante/cores", null),
        new Regra("CONFIG_BACKGROUND", "/api/auth/restaurante/background", null),
        // Ajuste manual do marcador de localização no mapa do Dashboard —
        // mesma permissão de Dados da Empresa, é o mesmo dado do restaurante.
        new Regra("CONFIG_DADOS_EMPRESA", "/api/auth/restaurante/localizacao", null),
        new Regra("CONFIG_DADOS_EMPRESA", "/api/auth/restaurante", ESCRITA),

        new Regra("CONFIG_STATUS_LOJA", "/api/configuracoes/status-loja", null),
        new Regra("CONFIG_ALERTA_PEDIDO", "/api/configuracoes/alerta-pedido", null),
        new Regra("CONFIG_PIX", "/api/configuracoes/pix", null),
        new Regra("CONFIG_COMISSOES", "/api/configuracoes/comissoes", null),
        new Regra("CONFIG_HORARIOS", "/api/configuracoes/horarios", null),
        new Regra("CONFIG_PAUSAS", "/api/configuracoes/pausas", null),
        new Regra("CONFIG_TAXAS_MAQUININHA", "/api/configuracoes/taxas-maquininha", null),

        new Regra("FINANCEIRO", "/api/financeiro", null),
        new Regra("FINANCEIRO", "/api/despesas", null),

        new Regra("SUPORTE", "/api/tickets", null),
        new Regra("USUARIOS", "/api/usuarios", null),
        new Regra("USUARIOS", "/api/grupos", null),
        // Sessão de caixa é um prefixo próprio, exclusivo do PDV — diferente
        // de /api/pedidos|comandas|entregas, dá pra isolar por permissão aqui.
        new Regra("CAIXA_PDV", "/api/caixa", null),

        // Leitura livre (usada por Garçom/Cozinha/PDV pra montar telas) —
        // só escrita exige a permissão do item Cardápio/Mesas.
        new Regra("MESAS", "/api/mesas", ESCRITA),
        new Regra("CARDAPIO", "/api/produtos", ESCRITA),
        new Regra("CARDAPIO", "/api/categorias", ESCRITA)
    );

    // Retorna a permissão exigida pra essa rota, ou null se nenhuma regra bate
    // (rota não gateada aqui — cai só no grant de role da Part A).
    public static String permissaoRequerida(String path, String metodo) {
        if (IGNORAR.stream().anyMatch(path::startsWith)) return null;
        for (Regra r : REGRAS) {
            if (!path.startsWith(r.prefixo())) continue;
            if (r.metodos() != null && !r.metodos().contains(metodo)) continue;
            return r.permissao();
        }
        return null;
    }
}
