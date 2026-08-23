package com.oiaaconta.table.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

// Sobrescreve os headers de identidade (X-User-Id, X-Restaurante-Id,
// X-User-Nome, X-User-Role) com os valores extraídos do JWT já validado,
// ignorando o que a requisição de fato trazia — sem isso, qualquer chamada
// com um JWT válido (de qualquer usuário) poderia forjar esses headers pra
// agir como outro usuário/tenant, caso conseguisse falar direto com o
// serviço (contornando o api-gateway). O gateway já define esses headers a
// partir do próprio token, mas nunca revalida contra eles — este wrapper faz
// o serviço ser a fonte final de verdade, independente do que chegou.
public class IdentidadeVerificadaRequest extends HttpServletRequestWrapper {

    private final Map<String, String> overrides = new LinkedHashMap<>();

    public IdentidadeVerificadaRequest(HttpServletRequest request, Long userId, Long restauranteId, String nome, String role) {
        super(request);
        if (userId != null) overrides.put("X-User-Id", String.valueOf(userId));
        if (restauranteId != null) overrides.put("X-Restaurante-Id", String.valueOf(restauranteId));
        if (nome != null) overrides.put("X-User-Nome", nome);
        if (role != null) overrides.put("X-User-Role", role);
    }

    private String overrideKey(String name) {
        for (String k : overrides.keySet()) {
            if (k.equalsIgnoreCase(name)) return k;
        }
        return null;
    }

    @Override
    public String getHeader(String name) {
        String key = overrideKey(name);
        return key != null ? overrides.get(key) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String key = overrideKey(name);
        return key != null
            ? Collections.enumeration(Collections.singletonList(overrides.get(key)))
            : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        var names = new LinkedHashSet<String>(overrides.keySet());
        Collections.list(super.getHeaderNames()).forEach(names::add);
        return Collections.enumeration(names);
    }
}
