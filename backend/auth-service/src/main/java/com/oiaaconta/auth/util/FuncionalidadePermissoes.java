package com.oiaaconta.auth.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Espelha FUNCIONALIDADES_DISPONIVEIS em GestorPlanos.tsx (frontend) — só as
// funcionalidades que correspondem a uma tela/permissão do painel entram no
// mapa; "Comandas ilimitadas", "Notificações em tempo real" e "Comissões de
// funcionários" são só texto de marketing do plano, sem tela própria pra
// travar, então não restringem nada aqui.
public final class FuncionalidadePermissoes {

    private FuncionalidadePermissoes() {}

    private static final Map<String, Set<String>> MAPA = Map.of(
        "Cardápio digital", Set.of("CARDAPIO"),
        "Delivery", Set.of("DELIVERY"),
        "Cozinha em tempo real", Set.of("COZINHA"),
        "Controle de mesas", Set.of("MESAS"),
        "PDV / Caixa", Set.of("CAIXA_PDV"),
        "Gestão de usuários", Set.of("USUARIOS"),
        "Relatórios financeiros", Set.of("FINANCEIRO"),
        "WhatsApp integrado", Set.of("WHATSAPP_CONEXAO", "WHATSAPP_MENSAGENS", "WHATSAPP_CONVERSAS"),
        "Painel de entregadores", Set.of("ENTREGADOR")
    );

    // Permissões que ALGUMA funcionalidade controla — as que não aparecem em
    // nenhuma entrada do MAPA (DASHBOARD, CLIENTES, GARCOM, COMANDA,
    // ESTOQUE, MARKETING, SUPORTE, IFOOD_CONEXAO, CONFIG_*) não são
    // restringíveis por plano e continuam liberadas pro grupo decidir.
    private static final Set<String> PERMISSOES_CONTROLADAS = MAPA.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toUnmodifiableSet());

    public static Set<String> aplicar(Set<String> permissoesDoGrupo, String funcionalidadesCsv) {
        if (permissoesDoGrupo == null) return null;
        // Sem plano ou plano sem funcionalidades cadastradas (contrato
        // ausente, restaurante em signup sem plano escolhido, plano legado
        // nunca preenchido): não restringe nada. Só passa a filtrar quando o
        // plano contratado tem uma lista de funcionalidades de fato definida.
        if (funcionalidadesCsv == null || funcionalidadesCsv.isBlank()) {
            return permissoesDoGrupo;
        }
        Set<String> liberadasPeloPlano = new HashSet<>();
        for (String f : funcionalidadesCsv.split(",")) {
            Set<String> perms = MAPA.get(f.trim());
            if (perms != null) liberadasPeloPlano.addAll(perms);
        }
        return permissoesDoGrupo.stream()
            .filter(p -> !PERMISSOES_CONTROLADAS.contains(p) || liberadasPeloPlano.contains(p))
            .collect(Collectors.toSet());
    }
}
