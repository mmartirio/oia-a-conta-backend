package com.oiaaconta.billing.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

// Isolamento entre restaurantes nos endpoints com id na URL: SUPER_ADMIN
// (suporte) vê tudo; os demais só o que é do próprio restaurante (header
// X-Restaurante-Id, preenchido pelo gateway a partir do JWT).
public final class AcessoTenant {

    private AcessoTenant() {}

    public static boolean isSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    // 404 (não 403) pra não confirmar que o recurso de outro restaurante existe.
    public static void exigirMesmoRestaurante(Long restauranteDoRecurso, Long restauranteDoUsuario) {
        if (isSuperAdmin()) return;
        if (restauranteDoUsuario == null || !Objects.equals(restauranteDoRecurso, restauranteDoUsuario)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
