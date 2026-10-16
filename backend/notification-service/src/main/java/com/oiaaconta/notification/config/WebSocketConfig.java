package com.oiaaconta.notification.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

// /ws é público no gateway (o handshake SockJS não carrega header de
// autenticação), então a autenticação acontece aqui, no frame STOMP:
// - CONNECT exige "Authorization: Bearer <jwt>" válido;
// - SUBSCRIBE só em /topic/{canal}/{restauranteId}/... do PRÓPRIO
//   restaurante (SUPER_ADMIN pode qualquer um).
// Antes, qualquer um assinava /topic/whatsapp/{id} e via ao vivo telefone e
// texto das conversas dos clientes, pedidos e o GPS dos entregadores.
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String ATTR_RESTAURANTE = "restauranteId";
    private static final String ATTR_SUPER_ADMIN = "superAdmin";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${ALLOWED_ORIGINS:http://localhost,http://localhost:80,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new))
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) return message;
                Map<String, Object> sessao = accessor.getSessionAttributes();
                if (sessao == null) throw new MessagingException("Sessão inválida");

                StompCommand comando = accessor.getCommand();
                if (comando == StompCommand.CONNECT) {
                    autenticar(accessor.getFirstNativeHeader("Authorization"), sessao);
                } else if (comando == StompCommand.SUBSCRIBE) {
                    autorizarAssinatura(accessor.getDestination(), sessao);
                } else if (comando == StompCommand.SEND) {
                    if (!sessao.containsKey(ATTR_SUPER_ADMIN)) throw new MessagingException("Não autenticado");
                }
                return message;
            }
        });
    }

    private void autenticar(String authorization, Map<String, Object> sessao) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessagingException("Token ausente");
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(authorization.substring(7))
                .getPayload();
            Object restauranteId = claims.get("restauranteId");
            if (restauranteId != null) sessao.put(ATTR_RESTAURANTE, String.valueOf(restauranteId));
            sessao.put(ATTR_SUPER_ADMIN, "SUPER_ADMIN".equals(String.valueOf(claims.get("role"))));
        } catch (Exception e) {
            log.warn("WebSocket CONNECT com token inválido: {}", e.getMessage());
            throw new MessagingException("Token inválido");
        }
    }

    private void autorizarAssinatura(String destino, Map<String, Object> sessao) {
        if (!sessao.containsKey(ATTR_SUPER_ADMIN)) throw new MessagingException("Não autenticado");
        if (Boolean.TRUE.equals(sessao.get(ATTR_SUPER_ADMIN))) return;
        // /topic/{canal}/{restauranteId}[/...]
        String[] partes = destino != null ? destino.split("/") : new String[0];
        if (partes.length < 4 || !"topic".equals(partes[1])
                || !partes[3].equals(sessao.get(ATTR_RESTAURANTE))) {
            log.warn("Assinatura WebSocket negada: {} (restaurante {})", destino, sessao.get(ATTR_RESTAURANTE));
            throw new MessagingException("Sem acesso a " + destino);
        }
    }
}
