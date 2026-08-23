package com.oiaaconta.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = auth.substring(7);
        if (jwtUtil.isValid(token)) {
            String email = jwtUtil.extractEmail(token);
            java.util.Set<String> permissoes = jwtUtil.extractPermissoes(token);
            List<SimpleGrantedAuthority> authorities = !permissoes.isEmpty()
                ? PermissaoRoles.derivar(permissoes).stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()
                : List.of(new SimpleGrantedAuthority("ROLE_" + jwtUtil.extractRole(token)));
            var authToken = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Blinda contra header forjado: os controllers leem X-User-Id/
            // X-Restaurante-Id/X-User-Role diretamente — sem isso, um JWT válido
            // de QUALQUER usuário bastaria pra passar por esses headers com
            // valores forjados e agir como outro usuário/tenant.
            String roleHeader = !permissoes.isEmpty()
                ? String.join(",", PermissaoRoles.derivar(permissoes))
                : jwtUtil.extractRole(token);
            request = new IdentidadeVerificadaRequest(request, jwtUtil.extractUserId(token),
                jwtUtil.extractRestauranteId(token), jwtUtil.extractNome(token), roleHeader);
        }
        chain.doFilter(request, response);
    }
}
