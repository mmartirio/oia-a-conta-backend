package com.oiaaconta.auth.security;

import com.oiaaconta.auth.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);
        if (!jwtUtil.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Blinda contra header forjado: os controllers leem X-User-Id/
            // X-Restaurante-Id/X-User-Role diretamente — sem isso, um JWT válido
            // de QUALQUER usuário bastaria pra passar por esses headers com
            // valores forjados e agir como outro usuário/tenant. Usa o próprio
            // registro do usuário (já revalidado no banco acima, via
            // loadUserByUsername) como fonte de verdade — mais autoritativo até
            // que o claim do token, porque reflete o estado atual (ex: grupo
            // trocado depois do token emitido), não o que valia na emissão.
            //
            // Roda fora de um @Transactional (é um Filter puro — self-invocation
            // de OncePerRequestFilter.doFilter não passa pelo proxy Spring, então
            // @Transactional aqui não teria efeito), então usuario.getGrupo()
            // precisa já vir carregado por findByEmail (ver @EntityGraph no
            // repository) — senão é LazyInitializationException na certa.
            try {
                var usuarioOpt = usuarioRepository.findByEmail(email);
                if (usuarioOpt.isPresent()) {
                    var usuario = usuarioOpt.get();
                    String roleHeader = usuario.getGrupo() != null
                        ? String.join(",", PermissaoRoles.derivar(usuario.getGrupo().getPermissoes()))
                        : usuario.getRole().name();
                    Long restauranteId = usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null;
                    request = new IdentidadeVerificadaRequest(
                        request, usuario.getId(), restauranteId, usuario.getNome(), roleHeader);
                }
            } catch (Exception e) {
                log.error("Falha ao revalidar identidade de {} — seguindo com os headers originais: {}", email, e.getMessage(), e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
