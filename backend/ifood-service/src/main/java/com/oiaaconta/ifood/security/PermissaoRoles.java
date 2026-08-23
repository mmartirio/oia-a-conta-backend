package com.oiaaconta.ifood.security;

import java.util.HashSet;
import java.util.Set;

// Deriva quais roles (Spring Security) um conjunto de permissões de Grupo
// concede — usado quando o usuário tem um grupo atribuído, substituindo o
// role fixo dele pra fins de autorização. Mesma tabela duplicada em cada
// microsserviço (auth, catalog, table, order, whatsapp, ifood) e no api-gateway.
public final class PermissaoRoles {

    private PermissaoRoles() { }

    public static Set<String> derivar(Set<String> permissoes) {
        Set<String> roles = new HashSet<>();
        if (permissoes == null) return roles;
        for (String p : permissoes) {
            switch (p) {
                case "COZINHA" -> roles.add("COZINHA");
                case "COMANDA", "GARCOM" -> roles.add("GARCON");
                case "DELIVERY" -> { roles.add("GARCON"); roles.add("COZINHA"); }
                case "ENTREGADOR" -> roles.add("ENTREGADOR");
                case "CAIXA_PDV" -> roles.add("CAIXA");
                default -> roles.add("ADMIN");
            }
        }
        return roles;
    }
}
